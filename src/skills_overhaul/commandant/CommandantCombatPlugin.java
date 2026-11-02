package skills_overhaul.commandant;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Commandant combat-script plugin. Effects keyed off ACTIVE_NODES — same pattern as
 * CorsairCombatPlugin. Owns IronDoctrineState lifecycle for the t5 capstone.
 */
public final class CommandantCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<>();

    private float combatTime = 0f;
    private final IronDoctrineState ironDoctrine = new IronDoctrineState();

    @Override
    public void init(CombatEngineAPI engine) {
        // Iron Doctrine registers these into the static CommandFieldService.PARAMS every
        // frame and nothing removes them at battle end — clear leftovers at battle start.
        CommandFieldService.unregisterParam("iron_doctrine_entrench");
        CommandFieldService.unregisterParam("iron_doctrine_total");
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        combatTime += amount;

        int playerHeld = ObjectiveTracker.countLive(0);
        int enemyHeld = ObjectiveTracker.countLive(1);
        int total = ObjectiveTracker.totalLive();
        boolean allHeld = (playerHeld == total) && (total > 0);
        ironDoctrine.tickConsecutiveAllHeld(amount, allHeld);
        ironDoctrine.tickMarchingGrace(amount, playerHeld > enemyHeld);

        // Iron Doctrine capstone state, read by applyIronDoctrine + per-ship Marching Advance.
        boolean marching = ironDoctrine.marchingActive(playerHeld, enemyHeld, ironDoctrine.isInMarchingGrace());
        boolean totalControl = ironDoctrine.totalControlActive();

        if (ACTIVE_NODES.contains("commandant_t5")) applyIronDoctrine(playerHeld, totalControl);

        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            applyPerShipEffects(s, engine, playerHeld, marching, totalControl);
        }
    }

    private void applyIronDoctrine(int objectivesHeld, boolean totalControl) {
        // Entrenched: Command Aura per-level scaling amplified +5% per held objective (max +25%).
        CommandFieldService.registerParam("iron_doctrine_entrench", "perLevelScalingMult",
                ironDoctrine.entrenchedMult(objectivesHeld));
        // Total Control: fleet aura cap +0.08 after holding every objective for 10 consecutive seconds.
        if (totalControl) {
            CommandFieldService.registerParam("iron_doctrine_total", "fleetCapBonus", 0.08f);
        } else {
            CommandFieldService.unregisterParam("iron_doctrine_total");
        }
    }

    private void applyPerShipEffects(ShipAPI s, CombatEngineAPI engine, int playerHeld,
                                     boolean marching, boolean totalControl) {
        if (ACTIVE_NODES.contains("commandant_t1_officer_training")) OfficerTraining.apply(s);
        if (ACTIVE_NODES.contains("commandant_t1_drilled")) DrilledAndReady.apply(s);
        if (ACTIVE_NODES.contains("commandant_t2_authority")) Authority.apply(s, playerHeld);
        if (ACTIVE_NODES.contains("commandant_t2_cohesion")) TacticalCohesion.apply(s, engine);
        if (ACTIVE_NODES.contains("commandant_t2_reserve")) ReserveCommitment.apply(s, combatTime);
        if (ACTIVE_NODES.contains("commandant_t3_line_anchor")) LineAndAnchor.apply(s, engine);
        if (ACTIVE_NODES.contains("commandant_t3_vanguard")) VanguardScreen.apply(s, engine);
        if (ACTIVE_NODES.contains("commandant_t5")) MarchingAdvance.apply(s, marching, totalControl);
    }

    /**
     * Iron Doctrine — Marching Advance: while the fleet holds objective majority (or within the
     * post-majority grace window), player ships gain +12% weapon damage and +10% movement speed,
     * doubled to +24%/+20% while Total Control is active.
     */
    static final class MarchingAdvance {
        static void apply(ShipAPI s, boolean marching, boolean totalControl) {
            if (!marching) {
                s.getMutableStats().getBallisticWeaponDamageMult().unmodify("commandant_t5_marching");
                s.getMutableStats().getEnergyWeaponDamageMult().unmodify("commandant_t5_marching");
                s.getMutableStats().getMaxSpeed().unmodify("commandant_t5_marching");
                return;
            }
            float dmg = totalControl ? 0.24f : 0.12f;
            float spd = totalControl ? 0.20f : 0.10f;
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("commandant_t5_marching", 1f + dmg);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("commandant_t5_marching", 1f + dmg);
            s.getMutableStats().getMaxSpeed().modifyMult("commandant_t5_marching", 1f + spd);
        }
    }

    /** Drilled and Ready: +5% weapon damage fleet-wide (vanilla Tactical Drills stats). */
    static final class DrilledAndReady {
        static void apply(ShipAPI s) {
            if (s.isFighter()) return;
            s.getMutableStats().getBallisticWeaponDamageMult().modifyPercent("commandant_t1_drilled", 5f);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyPercent("commandant_t1_drilled", 5f);
            s.getMutableStats().getMissileWeaponDamageMult().modifyPercent("commandant_t1_drilled", 5f);
        }
    }

    static final class OfficerTraining {
        static void apply(ShipAPI s) {
            if (s.getCaptain() == null || s.getCaptain().isDefault()) return;
            s.getMutableStats().getPeakCRDuration().modifyMult("commandant_t1_officer_training", 1.20f);
            s.getMutableStats().getMaxTurnRate().modifyMult("commandant_t1_officer_training", 1.10f);
        }
    }

    static final class Authority {
        static void apply(ShipAPI s, int objectivesHeld) {
            float k = 0.03f * objectivesHeld;
            s.getMutableStats().getMaxSpeed().modifyMult("commandant_t2_authority", 1f + k);
            s.getMutableStats().getMaxTurnRate().modifyMult("commandant_t2_authority", 1f + k);
            s.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("commandant_t2_authority", k * 100f);
            s.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("commandant_t2_authority", k * 100f);
        }
    }

    static final class TacticalCohesion {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            int nearby = 0;
            for (ShipAPI o : engine.getShips()) {
                if (o == s || o.getOwner() != s.getOwner()) continue;
                if (o.isFighter() || !o.isAlive()) continue; // fighters alone met the 2-friendly threshold
                if (Misc.getDistance(s.getLocation(), o.getLocation()) <= 2000f) nearby++;
            }
            if (nearby >= 2) {
                s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("commandant_t2_cohesion", 1.08f);
                s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("commandant_t2_cohesion", 1.08f);
                s.getMutableStats().getFluxDissipation().modifyMult("commandant_t2_cohesion", 1.08f);
            } else {
                s.getMutableStats().getBallisticWeaponDamageMult().unmodify("commandant_t2_cohesion");
                s.getMutableStats().getEnergyWeaponDamageMult().unmodify("commandant_t2_cohesion");
                s.getMutableStats().getFluxDissipation().unmodify("commandant_t2_cohesion");
            }
        }
    }

    static final class ReserveCommitment {
        static void apply(ShipAPI s, float combatTime) {
            if (combatTime > 60f && combatTime < 105f) {
                if (s.getHullSize() == ShipAPI.HullSize.FRIGATE
                        || s.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                    s.getMutableStats().getMaxSpeed().modifyMult("commandant_t2_reserve", 1.30f);
                } else {
                    s.getMutableStats().getMaxSpeed().modifyMult("commandant_t2_reserve", 1.15f);
                }
                s.getMutableStats().getMaxTurnRate().modifyMult("commandant_t2_reserve", 1.25f);
            } else {
                // The window closed but the buff was never removed — it lasted the rest of the battle.
                s.getMutableStats().getMaxSpeed().unmodify("commandant_t2_reserve");
                s.getMutableStats().getMaxTurnRate().unmodify("commandant_t2_reserve");
            }
        }
    }

    static final class LineAndAnchor {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            if (s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                s.getMutableStats().getMaxTurnRate().modifyMult("commandant_t3_line_anchor", 1.10f);
                return;
            } else if (s.getHullSize() == ShipAPI.HullSize.CRUISER) {
                s.getMutableStats().getArmorBonus().modifyMult("commandant_t3_line_anchor", 1.10f);
                s.getMutableStats().getFluxCapacity().modifyMult("commandant_t3_line_anchor", 1.20f);
                s.getMutableStats().getPeakCRDuration().modifyMult("commandant_t3_line_anchor", 1.25f);
            }
            // Receiver side: a non-capital within 1500u of a live friendly capital gets +20% range.
            // Previously applied per-source by each capital and never removed (out of range or after
            // the capital died), and stacked +20% per nearby capital.
            boolean anchored = false;
            for (ShipAPI o : engine.getShips()) {
                if (o == s || o.getOwner() != s.getOwner() || !o.isAlive()) continue;
                if (o.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP) continue;
                if (Misc.getDistance(s.getLocation(), o.getLocation()) > 1500f) continue;
                anchored = true;
                break;
            }
            if (anchored) {
                s.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("commandant_t3_line_anchor_range", 20f);
                s.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("commandant_t3_line_anchor_range", 20f);
            } else {
                s.getMutableStats().getBallisticWeaponRangeBonus().unmodify("commandant_t3_line_anchor_range");
                s.getMutableStats().getEnergyWeaponRangeBonus().unmodify("commandant_t3_line_anchor_range");
            }
        }
    }

    static final class VanguardScreen {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            if (s.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP
                    && s.getHullSize() != ShipAPI.HullSize.CRUISER) return;
            int escorts = 0;
            for (ShipAPI o : engine.getShips()) {
                if (o.getOwner() != s.getOwner()) continue;
                if (o.getHullSize() != ShipAPI.HullSize.FRIGATE
                        && o.getHullSize() != ShipAPI.HullSize.DESTROYER) continue;
                if (Misc.getDistance(s.getLocation(), o.getLocation()) > 1500f) continue;
                escorts++;
            }
            float k = Math.min(0.15f, 0.03f * escorts);
            s.getMutableStats().getFluxDissipation().modifyMult("commandant_t3_vanguard", 1f + k);
        }
    }
}
