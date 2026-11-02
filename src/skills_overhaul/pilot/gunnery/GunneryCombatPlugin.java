package skills_overhaul.pilot.gunnery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GunneryCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<String>();

    private ShipAPI flagship;
    private LockOnState lockOn;
    private ThreatAcquisitionState threatAcq;
    private KillingMachineState killingMachine;
    private BountyHunterState bountyHunter;
    private float hitAccum = 0f;
    private final IntervalUtil wolfpackInterval = new IntervalUtil(0.5f, 0.5f);
    private boolean initialized = false;
    private ShipAPI prevSurgicalTarget;
    private ShipAPI prevTwistKnifeTarget;

    /** Per-combat instance, so PilotXPTracker's kill detection can reach us. */
    private static GunneryCombatPlugin instance;

    public static void notifyKill(ShipAPI killed, float playerContribution) {
        if (instance != null && instance.initialized) {
            instance.onKill(killed, playerContribution);
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        instance = this;
        // ACTIVE_NODES is campaign-owned (populated at purchase / game load);
        // clearing here would wipe every gated skill for the whole battle.
    }

    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        // ACTIVE_NODES is campaign state; keep it out of title-screen/mission battles.
        if (!engine.isInCampaign() && !engine.isInCampaignSim()) return;
        if (!initialized) initState(engine);
        if (flagship == null || !flagship.isAlive()) return;

        String targetId = flagship.getShipTarget() != null
                ? flagship.getShipTarget().getId() : null;

        if (ACTIVE_NODES.contains("so_gunnery_lock_on"))
            lockOn.advance(dt, targetId);

        if (ACTIVE_NODES.contains("so_gunnery_threat_acq")) {
            boolean firing = isFiringAtTarget(flagship);
            if (firing) {
                hitAccum += dt;
                while (hitAccum >= 1f) { threatAcq.onHittingTarget(1f); hitAccum -= 1f; }
            } else {
                hitAccum = 0f;
                threatAcq.advanceNoHit(dt);
            }
        }

        if (ACTIVE_NODES.contains("so_gunnery_bounty_hunter"))
            bountyHunter.advance(dt);

        wolfpackInterval.advance(dt);
        applyMods(engine);
    }

    private boolean isFiringAtTarget(ShipAPI ship) {
        if (ship.getShipTarget() == null) return false;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isFiring()) return true;
        }
        return false;
    }

    private void applyMods(CombatEngineAPI engine) {
        MutableShipStatsAPI stats = flagship.getMutableStats();

        // Lock On: +5% damage, -5% flux cost when locked
        if (ACTIVE_NODES.contains("so_gunnery_lock_on") && lockOn.isLocked()) {
            stats.getBallisticWeaponDamageMult().modifyMult("so_lock_on", 1.05f);
            stats.getEnergyWeaponDamageMult().modifyMult("so_lock_on", 1.05f);
            stats.getMissileWeaponDamageMult().modifyMult("so_lock_on", 1.05f);
            stats.getBallisticWeaponFluxCostMod().modifyMult("so_lock_on", 0.95f);
            stats.getEnergyWeaponFluxCostMod().modifyMult("so_lock_on", 0.95f);
        } else {
            stats.getBallisticWeaponDamageMult().unmodify("so_lock_on");
            stats.getEnergyWeaponDamageMult().unmodify("so_lock_on");
            stats.getMissileWeaponDamageMult().unmodify("so_lock_on");
            stats.getBallisticWeaponFluxCostMod().unmodify("so_lock_on");
            stats.getEnergyWeaponFluxCostMod().unmodify("so_lock_on");
        }

        // Threat Acquisition: +1% damage/DR per stack, max 20%
        if (ACTIVE_NODES.contains("so_gunnery_threat_acq")) {
            float bonus = threatAcq.getBonusFraction();
            stats.getBallisticWeaponDamageMult().modifyMult("so_threat_acq", 1f + bonus);
            stats.getEnergyWeaponDamageMult().modifyMult("so_threat_acq", 1f + bonus);
            stats.getMissileWeaponDamageMult().modifyMult("so_threat_acq", 1f + bonus);
            stats.getHullDamageTakenMult().modifyMult("so_threat_acq", 1f - bonus);
        }

        // Killing Machine: +3% damage per kill-stack, −2% glass cannon penalty per stack
        if (ACTIVE_NODES.contains("so_gunnery_killing_machine")) {
            float dmg = killingMachine.getDamageBonus();
            stats.getBallisticWeaponDamageMult().modifyMult("so_killing_machine", 1f + dmg);
            stats.getEnergyWeaponDamageMult().modifyMult("so_killing_machine", 1f + dmg);
            stats.getMissileWeaponDamageMult().modifyMult("so_killing_machine", 1f + dmg);
            if (ACTIVE_NODES.contains("so_gunnery_glass_cannon")) {
                float relief = killingMachine.getGlassCannonPenaltyRelief();
                stats.getHullDamageTakenMult().modifyMult("so_glass_cannon_relief", 1f - relief);
            }
        }

        // Bounty Hunter: +20% flux dissipation for 8s after kill
        if (ACTIVE_NODES.contains("so_gunnery_bounty_hunter") && bountyHunter.isFluxBonusActive()) {
            stats.getFluxDissipation().modifyMult("so_bounty_hunter", 1.20f);
        } else {
            stats.getFluxDissipation().unmodify("so_bounty_hunter");
        }

        // Berserker: damage scales with flux level
        if (ACTIVE_NODES.contains("so_gunnery_berserker")) {
            float maxFlux = flagship.getFluxTracker().getMaxFlux();
            float fluxFrac = maxFlux > 0f ? flagship.getFluxTracker().getCurrFlux() / maxFlux : 0f;
            float bonus = fluxFrac * 0.20f;
            stats.getBallisticWeaponDamageMult().modifyMult("so_berserker", 1f + bonus);
            stats.getEnergyWeaponDamageMult().modifyMult("so_berserker", 1f + bonus);
            stats.getMissileWeaponDamageMult().modifyMult("so_berserker", 1f + bonus);
        }

        // Overload Hunter: +25% damage while target is overloaded
        if (ACTIVE_NODES.contains("so_gunnery_overload_hunter")) {
            ShipAPI target = flagship.getShipTarget();
            if (target != null && target.getFluxTracker().isOverloaded()) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_overload_hunter", 1.25f);
                stats.getEnergyWeaponDamageMult().modifyMult("so_overload_hunter", 1.25f);
                stats.getMissileWeaponDamageMult().modifyMult("so_overload_hunter", 1.25f);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_overload_hunter");
                stats.getEnergyWeaponDamageMult().unmodify("so_overload_hunter");
                stats.getMissileWeaponDamageMult().unmodify("so_overload_hunter");
            }
        }

        // Dragon Slayer: bonus vs larger hull sizes
        // Target null must fall through to the unmodify branch, or the bonus sticks.
        if (ACTIVE_NODES.contains("so_gunnery_dragon_slayer")) {
            int diff = flagship.getShipTarget() == null ? 0
                    : flagship.getShipTarget().getHullSize().ordinal() - flagship.getHullSize().ordinal();
            float bonus = Math.min(0.30f, Math.max(0f, diff * 0.10f));
            if (bonus > 0f) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_dragon_slayer", 1f + bonus);
                stats.getEnergyWeaponDamageMult().modifyMult("so_dragon_slayer", 1f + bonus);
                stats.getMissileWeaponDamageMult().modifyMult("so_dragon_slayer", 1f + bonus);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_dragon_slayer");
                stats.getEnergyWeaponDamageMult().unmodify("so_dragon_slayer");
                stats.getMissileWeaponDamageMult().unmodify("so_dragon_slayer");
            }
        }

        // Point Blank: within 400u → +20% damage, +50% weapon turn rate
        if (ACTIVE_NODES.contains("so_gunnery_point_blank")) {
            float dist = flagship.getShipTarget() == null ? Float.MAX_VALUE
                    : com.fs.starfarer.api.util.Misc.getDistance(
                    flagship.getLocation(), flagship.getShipTarget().getLocation());
            if (dist <= 400f) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_point_blank", 1.20f);
                stats.getEnergyWeaponDamageMult().modifyMult("so_point_blank", 1.20f);
                stats.getMissileWeaponDamageMult().modifyMult("so_point_blank", 1.20f);
                stats.getWeaponTurnRateBonus().modifyPercent("so_point_blank", 50f);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_point_blank");
                stats.getEnergyWeaponDamageMult().unmodify("so_point_blank");
                stats.getMissileWeaponDamageMult().unmodify("so_point_blank");
                stats.getWeaponTurnRateBonus().unmodifyPercent("so_point_blank");
            }
        }

        // === Gunnery PRD T1-T5 (2026-05-10) ===

        // Surgical Strikes (spec line 134): +30% damage to target's weapon
        // mounts and engines via target-side subsystem damage taken mults.
        // Approximation since DamageAPI lacks per-anatomy hit type.
        ShipAPI surgicalTarget = flagship.getShipTarget();
        if (ACTIVE_NODES.contains("so_gunnery_surgical")) {
            if (prevSurgicalTarget != null && prevSurgicalTarget != surgicalTarget) {
                MutableShipStatsAPI ts = prevSurgicalTarget.getMutableStats();
                ts.getWeaponDamageTakenMult().unmodify("so_surgical");
                ts.getEngineDamageTakenMult().unmodify("so_surgical");
            }
            if (surgicalTarget != null) {
                MutableShipStatsAPI ts = surgicalTarget.getMutableStats();
                ts.getWeaponDamageTakenMult().modifyMult("so_surgical", 1.30f);
                ts.getEngineDamageTakenMult().modifyMult("so_surgical", 1.30f);
            }
            prevSurgicalTarget = surgicalTarget;
        }

        // Twist the Knife (spec line 52): target <60% hull OR retreating →
        // attacker +25% dmg, target −15% speed, target ZFB removed.
        if (ACTIVE_NODES.contains("so_gunnery_twist_knife")) {
            // Clean the previous target's stats when the player switches targets
            // so the -15% speed / ZFB removal doesn't stick on the old ship.
            if (prevTwistKnifeTarget != null && prevTwistKnifeTarget != surgicalTarget) {
                MutableShipStatsAPI old = prevTwistKnifeTarget.getMutableStats();
                old.getMaxSpeed().unmodify("so_twist_knife_target");
                old.getZeroFluxSpeedBoost().unmodify("so_twist_knife_target");
            }
            if (surgicalTarget != null) {
                float hullFrac = surgicalTarget.getHullLevel();
                boolean retreating = surgicalTarget.isRetreating();
                boolean active = hullFrac < 0.60f || retreating;
                MutableShipStatsAPI ts = surgicalTarget.getMutableStats();
                if (active) {
                    stats.getBallisticWeaponDamageMult().modifyMult("so_twist_knife", 1.25f);
                    stats.getEnergyWeaponDamageMult().modifyMult("so_twist_knife", 1.25f);
                    stats.getMissileWeaponDamageMult().modifyMult("so_twist_knife", 1.25f);
                    ts.getMaxSpeed().modifyMult("so_twist_knife_target", 0.85f);
                    ts.getZeroFluxSpeedBoost().modifyMult("so_twist_knife_target", 0f);
                } else {
                    stats.getBallisticWeaponDamageMult().unmodify("so_twist_knife");
                    stats.getEnergyWeaponDamageMult().unmodify("so_twist_knife");
                    stats.getMissileWeaponDamageMult().unmodify("so_twist_knife");
                    ts.getMaxSpeed().unmodify("so_twist_knife_target");
                    ts.getZeroFluxSpeedBoost().unmodify("so_twist_knife_target");
                }
            } else {
                // No target: drop the attacker-side damage buff too.
                stats.getBallisticWeaponDamageMult().unmodify("so_twist_knife");
                stats.getEnergyWeaponDamageMult().unmodify("so_twist_knife");
                stats.getMissileWeaponDamageMult().unmodify("so_twist_knife");
            }
            prevTwistKnifeTarget = surgicalTarget;
        }

        // Dead Eye (spec line 142): static range + accuracy parts done in
        // SODefenseUnbreakableHandler-style handler (pending). Conditional
        // flux: −10% beyond 75% of weapon range.
        if (ACTIVE_NODES.contains("so_gunnery_dead_eye")) {
            float dist = surgicalTarget == null ? 0f
                    : com.fs.starfarer.api.util.Misc.getDistance(
                    flagship.getLocation(), surgicalTarget.getLocation());
            // Approx max range = 1000u; "75% of range" ≈ 750u.
            if (dist > 750f) {
                stats.getBallisticWeaponFluxCostMod().modifyMult("so_dead_eye_range", 0.90f);
                stats.getEnergyWeaponFluxCostMod().modifyMult("so_dead_eye_range", 0.90f);
            } else {
                stats.getBallisticWeaponFluxCostMod().unmodify("so_dead_eye_range");
                stats.getEnergyWeaponFluxCostMod().unmodify("so_dead_eye_range");
            }
            // Static parts (apply once, always-on while node active):
            stats.getBallisticWeaponRangeBonus().modifyPercent("so_dead_eye", 10f);
            stats.getEnergyWeaponRangeBonus().modifyPercent("so_dead_eye", 10f);
            stats.getAutofireAimAccuracy().modifyFlat("so_dead_eye", 1.0f);
        }

        // Weapons Discipline (spec line 58): suppress malfunction + amplify
        // existing flux reductions by +20%.
        if (ACTIVE_NODES.contains("so_gunnery_weapons_discipline")) {
            stats.getWeaponMalfunctionChance().modifyMult("so_weapons_disc", 0f);
            stats.getEngineMalfunctionChance().modifyMult("so_weapons_disc", 0f);
            stats.getCriticalMalfunctionChance().modifyMult("so_weapons_disc", 0f);
            stats.getBallisticWeaponFluxCostMod().modifyMult("so_weapons_disc", 0.80f);
            stats.getEnergyWeaponFluxCostMod().modifyMult("so_weapons_disc", 0.80f);
        }

        // Wolfpack: +5% per nearby ally, +1% per nearby fighter (polled every 0.5s)
        if (ACTIVE_NODES.contains("so_gunnery_wolfpack") && wolfpackInterval.intervalElapsed()) {
            int allies = 0, fighters = 0;
            for (ShipAPI s : engine.getShips()) {
                if (s == flagship || s.getOwner() != 0) continue;
                float dist = com.fs.starfarer.api.util.Misc.getDistance(flagship.getLocation(), s.getLocation());
                if (dist > 1750f) continue;
                if (s.getHullSize() == ShipAPI.HullSize.FIGHTER) fighters++; else allies++;
            }
            float bonus = allies * 0.05f + fighters * 0.01f;
            stats.getBallisticWeaponDamageMult().modifyMult("so_wolfpack", 1f + bonus);
            stats.getEnergyWeaponDamageMult().modifyMult("so_wolfpack", 1f + bonus);
            stats.getMissileWeaponDamageMult().modifyMult("so_wolfpack", 1f + bonus);
        }
    }

    public void onKill(ShipAPI killed, float playerContribution) {
        if (playerContribution < 0.15f) return;
        if (ACTIVE_NODES.contains("so_gunnery_threat_acq")) threatAcq.onKill();
        if (ACTIVE_NODES.contains("so_gunnery_killing_machine")) killingMachine.onKill();
        if (ACTIVE_NODES.contains("so_gunnery_bounty_hunter")) bountyHunter.onKill();
    }

    private void initState(CombatEngineAPI engine) {
        flagship = engine.getPlayerShip();
        // Retry next frame: latching initialized with a null flagship killed the lane all battle.
        if (flagship == null) return;
        lockOn = new LockOnState();
        threatAcq = new ThreatAcquisitionState();
        killingMachine = new KillingMachineState();
        bountyHunter = new BountyHunterState();
        initialized = true;
    }
}
