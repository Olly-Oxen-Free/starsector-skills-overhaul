package skills_overhaul.pilot.maneuvering;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ManeuveringCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<String>();

    private ShipAPI flagship;
    private ZFBThresholdManager zfbMgr;
    private VentTracker ventTracker;
    private PhaseStateTracker phaseTracker;
    private AceKillStacker aceStacker;
    private PerfectTimingTracker perfTiming;

    private float prevHull = -1f;
    private float shieldOfflineTimer = 0f;
    private float phaseDancerTimer = 0f;
    private float zfbConsecutiveSeconds = 0f;
    private float tacticalBreakTimer = 0f;
    private boolean tacticalBreakArmed = false;
    private boolean initialized = false;

    /** Per-combat instance, so PilotXPTracker's kill detection can reach us. */
    private static ManeuveringCombatPlugin instance;

    public static void notifyKill(ShipAPI killed, float playerContribution) {
        if (instance != null && instance.initialized) {
            instance.onKill(killed, playerContribution);
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        instance = this;
        // ACTIVE_NODES is campaign-owned — do not clear per combat.
    }

    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        // ACTIVE_NODES is campaign state; keep it out of title-screen/mission battles.
        if (!engine.isInCampaign() && !engine.isInCampaignSim()) return;
        if (!initialized) initState(engine);
        if (flagship == null || !flagship.isAlive()) return;

        float currFlux = flagship.getFluxTracker().getCurrFlux();
        float maxFlux  = flagship.getFluxTracker().getMaxFlux();
        float fluxFrac = maxFlux > 0f ? currFlux / maxFlux : 0f;
        boolean isVenting = flagship.getFluxTracker().isVenting();
        boolean isPhased  = flagship.isPhased();
        float currHull = flagship.getHitpoints();
        boolean tookDamage = prevHull > 0f && currHull < prevHull;

        if (ACTIVE_NODES.contains("so_maneuver_vent_sprint") || ACTIVE_NODES.contains("so_maneuver_deep_vent"))
            ventTracker.advance(dt, isVenting, currFlux, maxFlux);

        phaseTracker.advance(dt, isPhased);

        if (ACTIVE_NODES.contains("so_maneuver_ace"))
            aceStacker.advance(dt);

        if (ACTIVE_NODES.contains("so_maneuver_perfect_timing"))
            perfTiming.advance(dt, tookDamage);

        if (phaseDancerTimer > 0f) phaseDancerTimer -= dt;

        applyMods(engine, fluxFrac, isPhased, dt);
        prevHull = currHull;
    }

    private void applyMods(CombatEngineAPI engine, float fluxFrac, boolean isPhased, float dt) {
        MutableShipStatsAPI stats = flagship.getMutableStats();

        // Zero-Flux Mastery: speed + spread bonus while in ZFB
        if (ACTIVE_NODES.contains("so_maneuver_zfm") && zfbMgr.isInZFB(fluxFrac)) {
            stats.getMaxSpeed().modifyMult("so_zfm", 1.12f);
            stats.getRecoilPerShotMult().modifyMult("so_zfm", 0.85f);
        } else {
            stats.getMaxSpeed().unmodify("so_zfm");
            stats.getRecoilPerShotMult().unmodify("so_zfm");
        }

        // Vent Sprint: speed + turn rate during vent, scaled by flux at vent start
        if (ACTIVE_NODES.contains("so_maneuver_vent_sprint") && ventTracker.isVenting()) {
            stats.getMaxSpeed().modifyMult("so_vent_sprint", 1f + ventTracker.getVentSprintSpeedBonus());
            stats.getMaxTurnRate().modifyMult("so_vent_sprint", 1f + ventTracker.getVentSprintManeuverBonus());
        } else {
            stats.getMaxSpeed().unmodify("so_vent_sprint");
            stats.getMaxTurnRate().unmodify("so_vent_sprint");
        }

        // Deep Vent: +40% flux dissipation for first 2s of vent
        if (ACTIVE_NODES.contains("so_maneuver_deep_vent") && ventTracker.isDeepVentActive()) {
            stats.getFluxDissipation().modifyMult("so_deep_vent", 1.40f);
        } else {
            stats.getFluxDissipation().unmodify("so_deep_vent");
        }

        // Knife Fighter: +15% turn rate, +5% ballistic/energy ROF within 300u
        // Target null must fall through to the unmodify branch, or the bonus sticks.
        if (ACTIVE_NODES.contains("so_maneuver_knife_fighter")) {
            float dist = flagship.getShipTarget() == null ? Float.MAX_VALUE
                    : Misc.getDistance(flagship.getLocation(), flagship.getShipTarget().getLocation());
            if (dist <= 300f) {
                stats.getMaxTurnRate().modifyMult("so_knife_fighter", 1.15f);
                stats.getBallisticRoFMult().modifyMult("so_knife_fighter", 1.05f);
                stats.getEnergyRoFMult().modifyMult("so_knife_fighter", 1.05f);
            } else {
                stats.getMaxTurnRate().unmodify("so_knife_fighter");
                stats.getBallisticRoFMult().unmodify("so_knife_fighter");
                stats.getEnergyRoFMult().unmodify("so_knife_fighter");
            }
        }

        // Reactive Drive: acceleration bonus while phased or shield offline
        if (ACTIVE_NODES.contains("so_maneuver_reactive_drive")) {
            if (isPhased) {
                shieldOfflineTimer = 0f;
                stats.getAcceleration().modifyMult("so_reactive_drive", 1.20f);
            } else if (flagship.getShield() != null && !flagship.getShield().isOn()) {
                shieldOfflineTimer += dt;
                float bonus = Math.min(0.30f, shieldOfflineTimer * 0.05f);
                stats.getAcceleration().modifyMult("so_reactive_drive", 1f + bonus);
            } else {
                shieldOfflineTimer = 0f;
                stats.getAcceleration().unmodify("so_reactive_drive");
            }
        }

        // Phase Dancer: burst speed + turn rate for 5s after phase exit
        if (ACTIVE_NODES.contains("so_maneuver_phase_dancer") && phaseDancerTimer > 0f) {
            stats.getMaxSpeed().modifyMult("so_phase_dancer", 1.25f);
            stats.getMaxTurnRate().modifyMult("so_phase_dancer", 1.30f);
        } else {
            stats.getMaxSpeed().unmodify("so_phase_dancer");
            stats.getMaxTurnRate().unmodify("so_phase_dancer");
        }

        // Crossfire: +40% accel, +10% damage while strafing
        if (ACTIVE_NODES.contains("so_maneuver_crossfire")) {
            if (isStrafing(flagship)) {
                stats.getAcceleration().modifyMult("so_crossfire", 1.40f);
                stats.getBallisticWeaponDamageMult().modifyMult("so_crossfire", 1.10f);
                stats.getEnergyWeaponDamageMult().modifyMult("so_crossfire", 1.10f);
                stats.getMissileWeaponDamageMult().modifyMult("so_crossfire", 1.10f);
            } else {
                stats.getAcceleration().unmodify("so_crossfire");
                stats.getBallisticWeaponDamageMult().unmodify("so_crossfire");
                stats.getEnergyWeaponDamageMult().unmodify("so_crossfire");
                stats.getMissileWeaponDamageMult().unmodify("so_crossfire");
            }
        }

        // Kinetic Cooling: flux dissipation + damage scale with current speed
        if (ACTIVE_NODES.contains("so_maneuver_kinetic_cooling")) {
            float maxSpeed = stats.getMaxSpeed().getModifiedValue();
            float currSpeed = flagship.getVelocity() != null
                    ? (float) Math.sqrt(flagship.getVelocity().x * flagship.getVelocity().x
                    + flagship.getVelocity().y * flagship.getVelocity().y) : 0f;
            float speedFrac = maxSpeed > 0f ? Math.min(1f, currSpeed / maxSpeed) : 0f;
            boolean isSO = flagship.getVariant().hasHullMod("safetyoverrides");
            float dissBonus = speedFrac * (isSO ? 0.125f : 0.25f);
            stats.getFluxDissipation().modifyMult("so_kinetic_cooling", 1f + dissBonus);
            stats.getBallisticWeaponDamageMult().modifyMult("so_kinetic_cooling_dmg", 1f + speedFrac * 0.10f);
            stats.getEnergyWeaponDamageMult().modifyMult("so_kinetic_cooling_dmg", 1f + speedFrac * 0.10f);
            stats.getMissileWeaponDamageMult().modifyMult("so_kinetic_cooling_dmg", 1f + speedFrac * 0.10f);
        }

        // The Ace: turn rate scales with kill stacks
        if (ACTIVE_NODES.contains("so_maneuver_ace")) {
            float bonus = aceStacker.getManeuverBonus();
            if (bonus > 0f) {
                stats.getMaxTurnRate().modifyMult("so_ace", 1f + bonus);
            } else {
                stats.getMaxTurnRate().unmodify("so_ace");
            }
        }

        // Perfect Timing: +20% ROF after 6s no hull damage
        if (ACTIVE_NODES.contains("so_maneuver_perfect_timing") && perfTiming.isBuffActive()) {
            stats.getBallisticRoFMult().modifyMult("so_perfect_timing", 1.20f);
            stats.getEnergyRoFMult().modifyMult("so_perfect_timing", 1.20f);
            stats.getMissileRoFMult().modifyMult("so_perfect_timing", 1.20f);
            if (ACTIVE_NODES.contains("so_maneuver_battle_focus")) {
                stats.getMaxTurnRate().modifyMult("so_battle_focus", 1.20f);
                stats.getRecoilPerShotMult().modifyMult("so_battle_focus", 0.50f);
            }
        } else {
            stats.getBallisticRoFMult().unmodify("so_perfect_timing");
            stats.getEnergyRoFMult().unmodify("so_perfect_timing");
            stats.getMissileRoFMult().unmodify("so_perfect_timing");
            stats.getMaxTurnRate().unmodify("so_battle_focus");
            stats.getRecoilPerShotMult().unmodify("so_battle_focus");
        }

        // Flanking Mastery: +25% damage when attacking from the side or rear
        if (ACTIVE_NODES.contains("so_maneuver_flanking")) {
            if (flagship.getShipTarget() != null
                    && isFlankingAngle(angleBetween(flagship, flagship.getShipTarget()))) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_flanking", 1.25f);
                stats.getEnergyWeaponDamageMult().modifyMult("so_flanking", 1.25f);
                stats.getMissileWeaponDamageMult().modifyMult("so_flanking", 1.25f);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_flanking");
                stats.getEnergyWeaponDamageMult().unmodify("so_flanking");
                stats.getMissileWeaponDamageMult().unmodify("so_flanking");
            }
        }

        // Tactical Break: +25-30% damage burst triggered by firing after 3s in ZFB
        if (ACTIVE_NODES.contains("so_maneuver_tactical_break")) {
            if (zfbMgr.isInZFB(fluxFrac)) {
                zfbConsecutiveSeconds += dt;
            } else {
                zfbConsecutiveSeconds = 0f;
                tacticalBreakArmed = false;
            }
            if (zfbConsecutiveSeconds >= 3f && !tacticalBreakArmed) tacticalBreakArmed = true;

            if (tacticalBreakArmed && isFiring(flagship)) {
                tacticalBreakTimer = isPhased ? 6f : 5f;
                tacticalBreakArmed = false;
                zfbConsecutiveSeconds = 0f;
            }

            if (tacticalBreakTimer > 0f) {
                float mult = isPhased ? 1.30f : 1.25f;
                stats.getBallisticWeaponDamageMult().modifyMult("so_tactical_break", mult);
                stats.getEnergyWeaponDamageMult().modifyMult("so_tactical_break", mult);
                stats.getMissileWeaponDamageMult().modifyMult("so_tactical_break", mult);
                tacticalBreakTimer -= dt;
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_tactical_break");
                stats.getEnergyWeaponDamageMult().unmodify("so_tactical_break");
                stats.getMissileWeaponDamageMult().unmodify("so_tactical_break");
            }
        }

        // === Maneuvering PRD T2-T6 (2026-05-10) — 3 missing nodes + capstone ===

        // Stealth Adaptation (spec line 138): timescale +0.15 at >80% top speed
        // (normal); +0.25 while phased (phase ships).
        if (ACTIVE_NODES.contains("so_maneuver_stealth_adapt")) {
            float maxSpd = stats.getMaxSpeed().getModifiedValue();
            float currSpd = flagship.getVelocity().length();
            boolean phased = flagship.isPhased();
            float bonus = 0f;
            if (phased) bonus = 0.25f;
            else if (maxSpd > 0f && currSpd / maxSpd > 0.80f) bonus = 0.15f;
            if (bonus > 0f) {
                stats.getTimeMult().modifyPercent("so_stealth_adapt", bonus * 100f);
            } else {
                stats.getTimeMult().unmodify("so_stealth_adapt");
            }
        }

        // Phase Entry (spec line 139): phaseEntryFluxReduction handled
        // in SOPilotedStatHandler (T4 already added). First-phase-free flag
        // would need vanilla phase-cost intercept — deferred.

        // Ghost Protocol (spec line 105-107): suppress phase flux generation
        // while phased. Approximation: halve phase upkeep cost while phased.
        if (ACTIVE_NODES.contains("so_maneuver_ghost_protocol")) {
            if (flagship.isPhased()) {
                stats.getPhaseCloakUpkeepCostBonus().modifyMult("so_ghost_protocol", 0.10f);
            } else {
                stats.getPhaseCloakUpkeepCostBonus().unmodify("so_ghost_protocol");
            }
        }

        // The Untouchable (spec line 80-82): permanent ZFB while
        // firing/accelerating + 30% projectile negation (deferred — needs
        // DamageTakenModifier per-projectile gate). Phase ships: phase flux
        // costs removed.
        if (ACTIVE_NODES.contains("so_maneuver_untouchable")) {
            boolean firing = false;
            for (com.fs.starfarer.api.combat.WeaponAPI w : flagship.getAllWeapons()) {
                if (w.isFiring()) { firing = true; break; }
            }
            boolean accelerating = flagship.getEngineController() != null
                    && flagship.getEngineController().isAccelerating();
            if (firing || accelerating) {
                stats.getZeroFluxSpeedBoost().modifyFlat("so_untouchable_force_zfb", 50f);
            } else {
                stats.getZeroFluxSpeedBoost().unmodifyFlat("so_untouchable_force_zfb");
            }
            // Phase ships: zero phase flux costs.
            // Phase cloak is the ship's defense, not its system id (which can be null).
            if (flagship.getHullSpec().isPhase()) {
                stats.getPhaseCloakActivationCostBonus().modifyMult("so_untouchable_phase", 0f);
                stats.getPhaseCloakUpkeepCostBonus().modifyMult("so_untouchable_phase", 0f);
            }
        }
    }

    public void onKill(ShipAPI killed, float playerContribution) {
        if (playerContribution < 0.15f) return;
        if (!ACTIVE_NODES.contains("so_maneuver_ace")) return;
        if (killed.getHullSize() == ShipAPI.HullSize.FIGHTER) {
            aceStacker.onFighterKill();
        } else if (killed.getHullSize() == ShipAPI.HullSize.FRIGATE) {
            aceStacker.onFrigateKill();
        }
    }

    public PhaseStateTracker getPhaseTracker() { return phaseTracker; }

    private void initState(CombatEngineAPI engine) {
        flagship = engine.getPlayerShip();
        if (flagship == null) return;
        zfbMgr = new ZFBThresholdManager();
        if (ACTIVE_NODES.contains("so_maneuver_zfm"))        zfbMgr.applyZeroFluxMastery();
        if (ACTIVE_NODES.contains("so_maneuver_untouchable")) zfbMgr.applyUntouchable();
        ventTracker = new VentTracker();
        phaseTracker = new PhaseStateTracker();
        if (ACTIVE_NODES.contains("so_maneuver_phase_dancer")) {
            phaseTracker.addExitListener(duration -> {
                phaseDancerTimer = 5f;
            });
        }
        aceStacker = new AceKillStacker(hullClassMult(flagship));
        perfTiming = new PerfectTimingTracker();
        prevHull = flagship.getHitpoints();
        initialized = true;
    }

    private float hullClassMult(ShipAPI ship) {
        switch (ship.getHullSize()) {
            case DESTROYER:    return 0.90f;
            case CRUISER:      return 0.55f;
            case CAPITAL_SHIP: return 0.25f;
            default:           return 1.00f;
        }
    }

    private boolean isStrafing(ShipAPI ship) {
        if (ship.getVelocity() == null) return false;
        float vx = ship.getVelocity().x, vy = ship.getVelocity().y;
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        if (speed < 10f) return false;
        float angle = (float) Math.toDegrees(Math.atan2(vy, vx));
        float diff = Math.abs(angle - ship.getFacing()) % 360f;
        if (diff > 180f) diff = 360f - diff;
        return diff > 45f && diff < 135f;
    }

    private float angleBetween(ShipAPI attacker, ShipAPI target) {
        float dx = attacker.getLocation().x - target.getLocation().x;
        float dy = attacker.getLocation().y - target.getLocation().y;
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float diff = Math.abs(angle - target.getFacing()) % 360f;
        if (diff > 180f) diff = 360f - diff;
        return diff;
    }

    private boolean isFlankingAngle(float diff) {
        return diff > 45f;
    }

    private boolean isFiring(ShipAPI ship) {
        for (com.fs.starfarer.api.combat.WeaponAPI w : ship.getAllWeapons()) {
            if (w.isFiring()) return true;
        }
        return false;
    }
}
