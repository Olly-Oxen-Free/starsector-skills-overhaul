package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.pilot.maneuvering.ManeuveringCombatPlugin;
import skills_overhaul.pilot.maneuvering.PhaseStateTracker;

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

    // Phase-exit detection: track previous phased state
    private boolean wasPhased = false;
    // Shield-raised detection for Phase Coil
    private boolean shieldWasRaised = false;
    private float shieldLoweredTimer = 0f;

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        ACTIVE_NODES.clear();
        prevAliveEnemyIds.clear();
        gameTime = 0f;
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
            if (target != null && targetPainter.getStacks() > 0) {
                float dmg = 0f; // damage tracking done via flux delta below
                float shieldFlux = target.getFluxTracker().getCurrFlux();
                targetPainter.onShieldDamage(0f, gameTime); // tick only; actual damage from flux delta
                float debuff = targetPainter.getShieldEfficiencyDebuff();
                if (debuff > 0f) {
                    target.getMutableStats().getShieldDamageTakenMult().modifyMult("so_target_painter", 1f + debuff);
                }
            } else if (flagship.getShipTarget() == null) {
                // clear debuff from all ships when no target
                lastTargetId = null;
            }
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
        } else {
            flagship.getMutableStats().getBallisticWeaponDamageMult().unmodify("so_void_strike");
            flagship.getMutableStats().getEnergyWeaponDamageMult().unmodify("so_void_strike");
            flagship.getMutableStats().getMissileWeaponDamageMult().unmodify("so_void_strike");
            flagship.getMutableStats().getMissileWeaponDamageMult().unmodify("so_void_strike_ord");
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

        // Snapshot current alive enemies
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() == 1 && s.isAlive() && !s.isFighter()) {
                prevAliveEnemyIds.add(s.getId());
            }
        }
        initialized = true;
    }

    public TargetPainterState getTargetPainter() { return targetPainter; }
    public VoidStrikeWindow getVoidStrikeWindow() { return voidStrike; }
}
