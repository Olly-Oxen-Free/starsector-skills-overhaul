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

        if (ACTIVE_NODES.contains("commandant_t5")) applyIronDoctrine(playerHeld);

        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            applyPerShipEffects(s, engine, playerHeld);
        }
    }

    private void applyIronDoctrine(int objectivesHeld) {
        float entrenchedScalingBonus = 0.50f * (Math.min(5, objectivesHeld) / 5f);
        CommandFieldService.registerParam("iron_doctrine_entrench", "perLevelScalingMult", 1f + entrenchedScalingBonus);
        if (ironDoctrine.totalControlActive()) {
            CommandFieldService.registerParam("iron_doctrine_total", "fleetCapBonus", 0.08f);
        } else {
            CommandFieldService.unregisterParam("iron_doctrine_total");
        }
    }

    private void applyPerShipEffects(ShipAPI s, CombatEngineAPI engine, int playerHeld) {
        if (ACTIVE_NODES.contains("commandant_t1_officer_training")) OfficerTraining.apply(s);
        if (ACTIVE_NODES.contains("commandant_t1_standing_orders")) StandingOrders.apply(s);
        if (ACTIVE_NODES.contains("commandant_t2_authority")) Authority.apply(s, playerHeld);
        if (ACTIVE_NODES.contains("commandant_t2_cohesion")) TacticalCohesion.apply(s, engine);
        if (ACTIVE_NODES.contains("commandant_t2_reserve")) ReserveCommitment.apply(s, combatTime);
        if (ACTIVE_NODES.contains("commandant_t3_line_anchor")) LineAndAnchor.apply(s, engine);
        if (ACTIVE_NODES.contains("commandant_t3_vanguard")) VanguardScreen.apply(s, engine);
    }

    static final class OfficerTraining {
        static void apply(ShipAPI s) {
            if (s.getCaptain() == null || s.getCaptain().isDefault()) return;
            s.getMutableStats().getPeakCRDuration().modifyMult("commandant_t1_officer_training", 1.20f);
            s.getMutableStats().getMaxTurnRate().modifyMult("commandant_t1_officer_training", 1.10f);
        }
    }

    static final class StandingOrders {
        static void apply(ShipAPI s) {
            float per = (s.getCaptain() != null && !s.getCaptain().isDefault()) ? 4.5f : 3f;
            s.getMutableStats().getCRPerDeploymentPercent().modifyFlat("commandant_t1_standing_orders", per);
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
            }
        }
    }

    static final class LineAndAnchor {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            if (s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                s.getMutableStats().getMaxTurnRate().modifyMult("commandant_t3_line_anchor", 1.10f);
                for (ShipAPI o : engine.getShips()) {
                    if (o == s || o.getOwner() != s.getOwner()) continue;
                    if (o.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) continue;
                    if (Misc.getDistance(s.getLocation(), o.getLocation()) > 1500f) continue;
                    o.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("commandant_t3_line_anchor:" + s.getId(), 20f);
                    o.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("commandant_t3_line_anchor:" + s.getId(), 20f);
                }
            } else if (s.getHullSize() == ShipAPI.HullSize.CRUISER) {
                s.getMutableStats().getArmorBonus().modifyMult("commandant_t3_line_anchor", 1.10f);
                s.getMutableStats().getFluxCapacity().modifyMult("commandant_t3_line_anchor", 1.20f);
                s.getMutableStats().getPeakCRDuration().modifyMult("commandant_t3_line_anchor", 1.25f);
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
