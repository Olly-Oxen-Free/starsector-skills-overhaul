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
    private float prevShieldFlux = 0f;
    private float gameTime = 0f;

    private final Set<String> prevAliveEnemyIds = new HashSet<String>();
    private boolean initialized = false;

    private boolean wasPhased = false;
    private boolean shieldWasRaised = false;
    private float shieldLoweredTimer = 0f;

    // Secondary system (Experimental Overrides capstone)
    private SecondarySystemHullmod activeSecondary = null;
    private static final int SECONDARY_KEY = Keyboard.KEY_R;

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        ACTIVE_NODES.clear();
        prevAliveEnemyIds.clear();
        gameTime = 0f;
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        if (activeSecondary == null || flagship == null) return;
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
        if (!initialized) initState(engine);
        if (flagship == null || !flagship.isAlive()) return;

        gameTime += dt;

        detectKills(engine);

        boolean isPhased = flagship.isPhased();

        // Kill Sync: drain pending cooldown reduction
        if (ACTIVE_NODES.contains("so_systems_kill_sync") && killSync.getPendingReduction() > 0f) {
            float drained = killSync.drain(dt);
            ShipSystemAPI system = flagship.getSystem();
            if (system != null && system.getCooldownRemaining() > 0f) {
                system.setCooldownRemaining(Math.max(0f, system.getCooldownRemaining() - drained));
            }
        }

        // Reactive Systems: phase or shield-damage cooldown drain
        if (ACTIVE_NODES.contains("so_systems_reactive_systems")) {
            if (isPhased) {
                reactiveSystems.advancePhase(dt);
            } else {
                float shieldFlux = flagship.getFluxTracker().getCurrFlux();
                float shieldDmg = Math.max(0f, shieldFlux - prevShieldFlux);
                if (flagship.getShield() != null && flagship.getShield().isOn() && shieldDmg > 0f)
                    reactiveSystems.onShieldDamage(shieldDmg);
                prevShieldFlux = shieldFlux;
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

        // Phase-exit detection for Void Strike + Reactive Systems reset
        if (!isPhased && wasPhased) {
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
            if (targetId != null && !targetId.equals(lastTargetId)) targetPainter.onTargetSwitch();
            lastTargetId = targetId;

            ShipAPI target = flagship.getShipTarget();
            if (target != null) {
                // T1 bug fix: pass real flux delta as damage, not 0.
                float currFlux = target.getFluxTracker().getCurrFlux();
                float fluxDelta = Math.max(0f, currFlux - prevShieldFlux);
                if (fluxDelta > 0f) targetPainter.onShieldDamage(fluxDelta, gameTime);
                prevShieldFlux = currFlux;

                if (targetPainter.getStacks() > 0) {
                    float debuff = targetPainter.getShieldEfficiencyDebuff();
                    // Spec line 124: stat is getShieldEfficiencyMult, not Damage Taken.
                    target.getMutableStats().getShieldDamageTakenMult()
                            .modifyMult("so_target_painter", 1f + debuff);
                }
            } else {
                lastTargetId = null;
                prevShieldFlux = 0f;
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
                MutableShipStatsAPI ss = s.getMutableStats();
                ss.getBallisticWeaponDamageMult().modifyMult("so_strike_coord", 1.25f);
                ss.getEnergyWeaponDamageMult().modifyMult("so_strike_coord", 1.25f);
                ss.getMissileWeaponDamageMult().modifyMult("so_strike_coord", 1.25f);
                ss.getHullBonus().modifyPercent("so_strike_coord", 20f);
            }
        }

        // Phase Mastery (spec line 194): phase ships only — remove flux-speed
        // penalty while phased; phase-exit burst applied on phase exit below.
        if (ACTIVE_NODES.contains("so_systems_phase_mastery") && isPhased
                && flagship.getHullSpec().getShipSystemId().contains("phase")) {
            // Counter the vanilla phase speed penalty.
            flagship.getMutableStats().getMaxSpeed().modifyMult("so_phase_mastery", 1.50f);
        } else {
            flagship.getMutableStats().getMaxSpeed().unmodify("so_phase_mastery");
        }
        // Phase Mastery exit burst (spec line 194): on phase exit, spawn
        // 300 energy + 600 EMP scaled by exit flux.
        if (ACTIVE_NODES.contains("so_systems_phase_mastery") && wasPhased && !isPhased) {
            float fluxLevel = flagship.getFluxTracker().getFluxLevel();
            float scale = fluxLevel >= 0.80f ? 1.5f : (fluxLevel < 0.40f ? 0.75f : 1.0f);
            Global.getCombatEngine().spawnEmpArc(flagship, flagship.getLocation(), flagship, flagship,
                    com.fs.starfarer.api.combat.DamageType.ENERGY,
                    300f * scale, 600f * scale, 500f, "tachyon_lance_emp_impact",
                    15f, new java.awt.Color(180, 220, 255, 220),
                    new java.awt.Color(255, 255, 255, 255));
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
            if (s.getOwner() != 1 || s.isFighter()) continue;
            if (prevAliveEnemyIds.contains(s.getId()) && !s.isAlive()) {
                float killedDP = s.getFleetMember() != null ? s.getFleetMember().getDeploymentPointsCost() : 10f;
                float flagshipDP = flagship.getFleetMember() != null ? flagship.getFleetMember().getDeploymentPointsCost() : 10f;
                if (killSyncActive) {
                    int cls = killClass(s);
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

    private boolean isPhaseShip(ShipAPI ship) {
        ShipSystemAPI sys = ship.getSystem();
        return sys != null && (sys.getId().contains("phase") || sys.getId().contains("phasecloak"));
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
