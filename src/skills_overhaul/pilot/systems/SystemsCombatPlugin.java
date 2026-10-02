package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;
import skills_overhaul.pilot.maneuvering.ManeuveringCombatPlugin;
import skills_overhaul.pilot.maneuvering.PhaseStateTracker;
import skills_overhaul.pilot.systems.secondary.*;

import java.util.*;

public final class SystemsCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<String>();

    private ShipAPI flagship;
    private KillSyncTracker killSync;
    private ReactiveSystemsTracker reactiveSystems;
    private RedLineStacker redLine;
    private VoidStrikeWindow voidStrike;
    private TargetPainterState targetPainter;

    private String lastTargetId = null;
    private ShipAPI paintedTarget = null;
    private float prevOwnShieldFlux = 0f;
    private float prevTargetShieldFlux = 0f;
    private float gameTime = 0f;

    // Strike Coordination: allied bombers within this radius get the buff (spec unspecified).
    private static final float STRIKE_COORD_RANGE = 2000f;

    private final Set<String> prevAliveEnemyIds = new HashSet<String>();
    private boolean initialized = false;

    private boolean wasPhased = false;
    private boolean shieldWasRaised = false;
    private float shieldLoweredTimer = 0f;

    // Precision Strike (T4): armor-penetration DamageDealtModifier is attached
    // once per battle to the flagship while the node is active.
    private boolean precisionStrikeAttached = false;

    // Secondary system (Experimental Overrides capstone)
    private SecondarySystemHullmod activeSecondary = null;
    private static final int SECONDARY_KEY = Keyboard.KEY_R;

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        // ACTIVE_NODES is campaign-owned — do not clear per combat.
        prevAliveEnemyIds.clear();
        gameTime = 0f;
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        // Same gate as advance(): without the node (or a live flagship) the secondary
        // would fire but never tick its duration/cleanup.
        if (activeSecondary == null || flagship == null || !flagship.isAlive()) return;
        if (!ACTIVE_NODES.contains("so_systems_experimental_overrides")) return;
        for (InputEventAPI event : events) {
            if (event.isKeyDownEvent() && event.getEventValue() == SECONDARY_KEY) {
                activeSecondary.activate(flagship, Global.getCombatEngine());
                if (ACTIVE_NODES.contains("so_systems_kill_sync")) killSync.onSecondaryActivation();
            }
        }
    }

    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        // ACTIVE_NODES is campaign state; keep it out of title-screen/mission battles.
        if (!engine.isInCampaign() && !engine.isInCampaignSim()) return;
        if (!initialized) initState(engine);
        if (flagship == null) return;
        if (!flagship.isAlive()) {
            // Secondaries stop ticking with the flagship; don't leave Domain
            // Overclock's engine-clock change applied for the rest of the battle.
            engine.getTimeMult().unmodify(SODomainOverclock.ID);
            return;
        }

        gameTime += dt;

        detectKills(engine);

        boolean isPhased = flagship.isPhased();

        // Kill Sync: drain pending cooldown reduction
        if (ACTIVE_NODES.contains("so_systems_kill_sync") && killSync.getPendingReduction() > 0f) {
            float drained = killSync.drain(dt);
            ShipSystemAPI system = flagship.getSystem();
            // Design rule: never accelerate limited-use systems. usesAmmo() is true
            // for any system with charges/uses (maxUses != Integer.MAX_VALUE).
            if (system != null && !system.getSpecAPI().usesAmmo()
                    && system.getCooldownRemaining() > 0f) {
                system.setCooldownRemaining(Math.max(0f, system.getCooldownRemaining() - drained));
            }
        }

        // Reactive Systems: phase or shield-damage cooldown drain
        if (ACTIVE_NODES.contains("so_systems_reactive_systems")) {
            if (isPhased) {
                reactiveSystems.advancePhase(dt);
            } else {
                // Hard flux only: shield hits generate hard flux, own weapon fire soft.
                float shieldFlux = flagship.getFluxTracker().getHardFlux();
                float shieldDmg = Math.max(0f, shieldFlux - prevOwnShieldFlux);
                if (flagship.getShield() != null && flagship.getShield().isOn() && shieldDmg > 0f)
                    reactiveSystems.onShieldDamage(shieldDmg);
                prevOwnShieldFlux = shieldFlux;
            }
            float drained = reactiveSystems.drainCooldownReduction(dt);
            if (drained > 0f && flagship.getSystem() != null)
                flagship.getSystem().setCooldownRemaining(
                    Math.max(0f, flagship.getSystem().getCooldownRemaining() - drained));
        }

        // Red Line: stack timers + flux diss bonus
        if (ACTIVE_NODES.contains("so_systems_red_line")) {
            redLine.advance(dt);
            float dissBonus = redLine.getFluxDissBonus();
            if (dissBonus > 0f) {
                flagship.getMutableStats().getFluxDissipation().modifyMult("so_red_line", 1f + dissBonus);
            } else {
                flagship.getMutableStats().getFluxDissipation().unmodify("so_red_line");
            }
            // T1 bug fix: getCRBonus computed but never applied.
            float crBonus = redLine.getCRBonus();
            if (crBonus > 0f) {
                flagship.getMutableStats().getCRPerDeploymentPercent()
                        .modifyFlat("so_red_line_cr", crBonus * 100f);
            } else {
                flagship.getMutableStats().getCRPerDeploymentPercent().unmodifyFlat("so_red_line_cr");
            }
        }

        // Void Strike: advance window, apply/remove damage mods
        if (ACTIVE_NODES.contains("so_systems_void_strike")) {
            voidStrike.advance(dt);
            applyVoidStrikeMods();
        }

        // Phase-exit detection for Void Strike + Reactive Systems reset.
        // Captured before wasPhased is overwritten so Phase Mastery below can see it.
        boolean phaseExited = !isPhased && wasPhased;
        if (phaseExited) {
            if (ACTIVE_NODES.contains("so_systems_void_strike")) voidStrike.trigger();
            if (ACTIVE_NODES.contains("so_systems_reactive_systems")) reactiveSystems.onPhaseExit();
        }
        wasPhased = isPhased;

        // Phase Coil: shield-lowered timer + weapon flux cost reduction
        if (ACTIVE_NODES.contains("so_systems_phase_coil")) {
            boolean shieldOn = flagship.getShield() != null && flagship.getShield().isOn();
            if (shieldOn) { shieldWasRaised = true; shieldLoweredTimer = 0f; }
            else if (shieldWasRaised) { shieldLoweredTimer += dt; }
            boolean inWindow = isPhased || (shieldWasRaised && shieldLoweredTimer < 2.5f);
            if (inWindow) {
                float mult = isPhased ? 0.80f : 0.85f;
                flagship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult("so_phase_coil", mult);
                flagship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult("so_phase_coil", mult);
                flagship.getMutableStats().getMissileWeaponFluxCostMod().modifyMult("so_phase_coil", mult);
            } else {
                flagship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify("so_phase_coil");
                flagship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify("so_phase_coil");
                flagship.getMutableStats().getMissileWeaponFluxCostMod().unmodify("so_phase_coil");
            }
        }

        // Flux Exploitation: energy weapon bonus at high flux
        if (ACTIVE_NODES.contains("so_systems_flux_exploitation")) {
            float fluxFrac = flagship.getFluxTracker().getMaxFlux() > 0f
                    ? flagship.getFluxTracker().getCurrFlux() / flagship.getFluxTracker().getMaxFlux() : 0f;
            if (fluxFrac >= 0.60f) {
                flagship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("so_flux_exploit", 1.15f);
                flagship.getMutableStats().getBeamWeaponFluxCostMult().modifyMult("so_flux_exploit_beam", 0.90f);
            } else {
                flagship.getMutableStats().getEnergyWeaponDamageMult().unmodify("so_flux_exploit");
                flagship.getMutableStats().getBeamWeaponFluxCostMult().unmodify("so_flux_exploit_beam");
            }
        }

        // Target Painter: track target switches + apply shield debuff on target
        if (ACTIVE_NODES.contains("so_systems_target_painter")) {
            String targetId = flagship.getShipTarget() != null ? flagship.getShipTarget().getId() : null;
            if (targetId != null && !targetId.equals(lastTargetId)) {
                targetPainter.onTargetSwitch();
                // New target → reset its flux baseline so the first delta isn't garbage.
                prevTargetShieldFlux = 0f;
            }
            lastTargetId = targetId;

            ShipAPI target = flagship.getShipTarget();
            // Stacks reset on switch, so strip the debuff from the previous target.
            if (paintedTarget != null && paintedTarget != target) {
                paintedTarget.getMutableStats().getShieldDamageTakenMult().unmodify("so_target_painter");
                paintedTarget = null;
            }
            if (target != null) {
                // T1 bug fix: pass real flux delta as damage, not 0.
                // Hard flux only: the target's own weapon fire adds soft flux.
                float currFlux = target.getFluxTracker().getHardFlux();
                float fluxDelta = Math.max(0f, currFlux - prevTargetShieldFlux);
                if (fluxDelta > 0f) targetPainter.onShieldDamage(fluxDelta, gameTime);
                prevTargetShieldFlux = currFlux;

                if (targetPainter.getStacks() > 0) {
                    float debuff = targetPainter.getShieldEfficiencyDebuff();
                    // Spec line 124: stat is getShieldEfficiencyMult, not Damage Taken.
                    target.getMutableStats().getShieldDamageTakenMult()
                            .modifyMult("so_target_painter", 1f + debuff);
                    paintedTarget = target;
                }
            } else {
                lastTargetId = null;
                prevTargetShieldFlux = 0f;
            }
        }

        if (activeSecondary != null && ACTIVE_NODES.contains("so_systems_experimental_overrides")) {
            activeSecondary.advance(dt, flagship, Global.getCombatEngine());
        }

        // === Systems PRD T2-T3 (2026-05-10) — 4 missing nodes ===

        // EMP Specialist (spec line 178): +20% EMP damage (always-on while active).
        // Per-EMP-hit weapon turn debuff on target deferred (no clean detection).
        if (ACTIVE_NODES.contains("so_systems_emp_specialist")) {
            flagship.getMutableStats().getEnergyWeaponDamageMult()
                    .modifyMult("so_emp_spec_dmg", 1.20f);
        } else {
            flagship.getMutableStats().getEnergyWeaponDamageMult().unmodify("so_emp_spec_dmg");
        }

        // Strike Coordination (spec line 182): allied bombers within range get
        // +25% damage / +20% hull. Bomber-hit-on-target debuff deferred (no
        // clean per-weapon-type damage detection).
        if (ACTIVE_NODES.contains("so_systems_strike_coord")) {
            for (ShipAPI s : Global.getCombatEngine().getShips()) {
                if (s == flagship || s.getOwner() != flagship.getOwner()) continue;
                if (s.getHullSize() != ShipAPI.HullSize.FIGHTER) continue;
                if (!s.isFighter()) continue;
                // Spec: bombers only (was buffing every allied fighter).
                if (s.getWing() == null
                        || s.getWing().getRole() != com.fs.starfarer.api.loading.WingRole.BOMBER) continue;
                MutableShipStatsAPI ss = s.getMutableStats();
                // Honour the "within range" comment: only allied bombers inside the
                // coordination radius get the buff; strip it once they fall out.
                boolean inRange = com.fs.starfarer.api.util.Misc.getDistance(
                        flagship.getLocation(), s.getLocation()) <= STRIKE_COORD_RANGE;
                if (inRange) {
                    ss.getBallisticWeaponDamageMult().modifyMult("so_strike_coord", 1.25f);
                    ss.getEnergyWeaponDamageMult().modifyMult("so_strike_coord", 1.25f);
                    ss.getMissileWeaponDamageMult().modifyMult("so_strike_coord", 1.25f);
                    ss.getHullBonus().modifyPercent("so_strike_coord", 20f);
                } else {
                    ss.getBallisticWeaponDamageMult().unmodify("so_strike_coord");
                    ss.getEnergyWeaponDamageMult().unmodify("so_strike_coord");
                    ss.getMissileWeaponDamageMult().unmodify("so_strike_coord");
                    ss.getHullBonus().unmodifyPercent("so_strike_coord");
                }
            }
        }

        // Phase Mastery (spec line 194): phase ships only — remove flux-speed
        // penalty while phased; phase-exit burst applied on phase exit below.
        if (ACTIVE_NODES.contains("so_systems_phase_mastery") && isPhased
                && isPhaseShip(flagship)) {
            // Counter the vanilla phase speed penalty.
            flagship.getMutableStats().getMaxSpeed().modifyMult("so_phase_mastery", 1.50f);
        } else {
            flagship.getMutableStats().getMaxSpeed().unmodify("so_phase_mastery");
        }
        // Phase Mastery exit burst (spec line 194): on phase exit, spawn
        // 300 energy + 600 EMP scaled by exit flux.
        // (wasPhased was already updated above, so it can't detect the exit here.)
        if (ACTIVE_NODES.contains("so_systems_phase_mastery") && phaseExited) {
            float fluxLevel = flagship.getFluxTracker().getFluxLevel();
            float scale = fluxLevel >= 0.80f ? 1.5f : (fluxLevel < 0.40f ? 0.75f : 1.0f);
            // Arc at the nearest enemy; targeting the flagship itself damaged the player.
            ShipAPI arcTarget = com.fs.starfarer.api.util.Misc.findClosestShipEnemyOf(
                    flagship, flagship.getLocation(), ShipAPI.HullSize.FIGHTER, 500f, true);
            if (arcTarget != null) {
                Global.getCombatEngine().spawnEmpArc(flagship, flagship.getLocation(), flagship, arcTarget,
                        com.fs.starfarer.api.combat.DamageType.ENERGY,
                        300f * scale, 600f * scale, 500f, "tachyon_lance_emp_impact",
                        15f, new java.awt.Color(180, 220, 255, 220),
                        new java.awt.Color(255, 255, 255, 255));
            }
        }

        // Carrier Command (spec line 198): bomber-target-kill teleport-home.
        // Detection requires per-bomber kill tracking; deferred to follow-up.
        // Currently provides nominal carrier replacement-rate buff.
        if (ACTIVE_NODES.contains("so_systems_carrier_command")) {
            flagship.getMutableStats().getFighterRefitTimeMult()
                    .modifyMult("so_carrier_command", 0.80f);
        } else {
            flagship.getMutableStats().getFighterRefitTimeMult().unmodify("so_carrier_command");
        }

        // Precision Strike (spec line 196): range-scaled armor penetration on
        // energy weapons. Implemented as a DamageDealtModifier (see
        // PrecisionStrikeListener) attached once per battle to the flagship.
        if (ACTIVE_NODES.contains("so_systems_precision_strike") && !precisionStrikeAttached) {
            flagship.addListener(new PrecisionStrikeListener(flagship));
            precisionStrikeAttached = true;
        }

        // Phase Resonance merge (spec line 202): intentional no-op stub in v1.
        // The node registers "so_systems_phase_resonance_merge" and displays a
        // "Coming Soon" tooltip; no combat effect is defined by the spec, so
        // there is deliberately nothing to apply here.
    }

    private void applyVoidStrikeMods() {
        if (voidStrike.isActive()) {
            float allBonus = 1f + voidStrike.getAllWeaponDamageBonus();
            flagship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("so_void_strike", allBonus);
            flagship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("so_void_strike", allBonus);
            flagship.getMutableStats().getMissileWeaponDamageMult().modifyMult("so_void_strike", allBonus);
            flagship.getMutableStats().getMissileWeaponDamageMult().modifyMult("so_void_strike_ord",
                1f + voidStrike.getOrdnanceDamageBonus());
            // T1 bug fix: ordnance speed bonus computed but never applied.
            flagship.getMutableStats().getProjectileSpeedMult().modifyMult(
                    "so_void_strike_speed", 1f + voidStrike.getOrdnanceSpeedBonus());
        } else {
            flagship.getMutableStats().getBallisticWeaponDamageMult().unmodify("so_void_strike");
            flagship.getMutableStats().getEnergyWeaponDamageMult().unmodify("so_void_strike");
            flagship.getMutableStats().getMissileWeaponDamageMult().unmodify("so_void_strike");
            flagship.getMutableStats().getMissileWeaponDamageMult().unmodify("so_void_strike_ord");
            flagship.getMutableStats().getProjectileSpeedMult().unmodify("so_void_strike_speed");
        }
    }

    private void detectKills(CombatEngineAPI engine) {
        boolean killSyncActive = ACTIVE_NODES.contains("so_systems_kill_sync");
        boolean redLineActive  = ACTIVE_NODES.contains("so_systems_red_line");
        if (!killSyncActive && !redLineActive) return;

        Set<String> nowAlive = new HashSet<String>();
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() == 1 && s.isAlive() && !s.isFighter()) {
                nowAlive.add(s.getId());
            }
        }
        // Ships that were alive last frame but aren't now = kills
        for (ShipAPI s : engine.getShips()) {
            // No owner check: hulks switch to owner 100, so `getOwner() != 1` skipped
            // every kill. prevAliveEnemyIds already holds only live enemy non-fighters.
            if (s.isFighter()) continue;
            if (prevAliveEnemyIds.contains(s.getId()) && !s.isAlive()) {
                float killedDP = s.getFleetMember() != null ? s.getFleetMember().getDeploymentPointsCost() : 10f;
                float flagshipDP = flagship.getFleetMember() != null ? flagship.getFleetMember().getDeploymentPointsCost() : 10f;
                if (killSyncActive) {
                    // Design rule: the hull-class scalar penalises the player's own
                    // large hull (capital farming frigates), not the size of the kill.
                    int cls = killClass(flagship);
                    killSync.applyReduction(killSync.computeReduction(killedDP, flagshipDP, cls));
                }
                if (redLineActive) redLine.onKill();
            }
        }
        prevAliveEnemyIds.clear();
        prevAliveEnemyIds.addAll(nowAlive);
    }

    public void onSystemUse() {
        if (ACTIVE_NODES.contains("so_systems_void_strike")) voidStrike.trigger();
        if (ACTIVE_NODES.contains("so_systems_kill_sync")) killSync.onSecondaryActivation();
    }

    private int killClass(ShipAPI ship) {
        switch (ship.getHullSize()) {
            case CRUISER:      return KillSyncTracker.CLASS_CRUISER;
            case CAPITAL_SHIP: return KillSyncTracker.CLASS_CAPITAL;
            default:           return KillSyncTracker.CLASS_FRIGATE;
        }
    }

    /**
     * Unified phase-ship detection (hull-spec based, matching ManeuveringCombatPlugin
     * and the Phase Mastery site above). The phase cloak is the hull's defense, not its
     * ship system, so use the vanilla idiom getHullSpec().isPhase().
     */
    private static boolean isPhaseShip(ShipAPI ship) {
        return ship != null
                && ship.getHullSpec() != null
                && ship.getHullSpec().isPhase();
    }

    private int hullClassIndex(ShipAPI ship) {
        switch (ship.getHullSize()) {
            case FRIGATE:      return ReactiveSystemsTracker.HULL_FRIGATE;
            case DESTROYER:    return ReactiveSystemsTracker.HULL_DESTROYER;
            case CRUISER:      return ReactiveSystemsTracker.HULL_CRUISER;
            case CAPITAL_SHIP: return ReactiveSystemsTracker.HULL_CAPITAL;
            default:           return ReactiveSystemsTracker.HULL_FRIGATE;
        }
    }

    private void initState(CombatEngineAPI engine) {
        flagship = engine.getPlayerShip();
        if (flagship == null) return;
        boolean phase = isPhaseShip(flagship);
        int hullClass = hullClassIndex(flagship);
        killSync       = new KillSyncTracker();
        reactiveSystems = new ReactiveSystemsTracker(phase, hullClass);
        redLine        = new RedLineStacker();
        voidStrike     = new VoidStrikeWindow(phase);
        targetPainter  = new TargetPainterState();
        wasPhased      = flagship.isPhased();
        shieldWasRaised = flagship.getShield() != null && flagship.getShield().isOn();
        precisionStrikeAttached = false;

        activeSecondary = detectSecondaryHullmod(flagship);

        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() == 1 && s.isAlive() && !s.isFighter()) {
                prevAliveEnemyIds.add(s.getId());
            }
        }
        initialized = true;
    }

    private SecondarySystemHullmod detectSecondaryHullmod(ShipAPI ship) {
        if (ship == null) return null;
        if (ship.getVariant().hasHullMod("so_secondary_domain_overclock")) return new SODomainOverclock();
        if (ship.getVariant().hasHullMod("so_secondary_flux_dissolver"))   return new SOFluxDissolverBattery();
        if (ship.getVariant().hasHullMod("so_secondary_reflector"))        return new SOReflectorProtocol();
        if (ship.getVariant().hasHullMod("so_secondary_dissonance"))       return new SODissonanceProjector();
        if (ship.getVariant().hasHullMod("so_secondary_skimmer"))          return new SODimensionalSkimmer();
        if (ship.getVariant().hasHullMod("so_secondary_stasis"))           return new SOStasisEmitter();
        if (ship.getVariant().hasHullMod("so_secondary_snapshot"))         return new SOCombatSnapshot();
        if (ship.getVariant().hasHullMod("so_secondary_ghost"))            return new SOFluxGhostProjector();
        return null;
    }

    public TargetPainterState getTargetPainter() { return targetPainter; }
    public VoidStrikeWindow getVoidStrikeWindow() { return voidStrike; }
}
