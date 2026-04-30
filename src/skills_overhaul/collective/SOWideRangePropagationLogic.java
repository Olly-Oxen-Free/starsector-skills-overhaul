package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class SOWideRangePropagationLogic {

    private static final String SRC = "so_wide_range_propagation";

    private final FighterInheritanceTracker tracker;

    public SOWideRangePropagationLogic(FighterInheritanceTracker tracker) {
        this.tracker = tracker;
    }

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_wide_range_propagation")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive()) continue;
            if (!ship.isFighter()) continue;
            if (ship.getWing() == null) continue;
            if (ship.getWing().getSpec() == null) continue;

            ShipAPI source = ship.getWing().getSourceShip();
            if (source == null) continue;
            if (!isAICored(source)) continue;

            if (!tracker.markIfNew(ship.getId())) continue;

            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getMaxSpeed().modifyPercent(SRC, 10f);
            stats.getBallisticWeaponDamageMult().modifyPercent(SRC, 5f);
            stats.getEnergyWeaponDamageMult().modifyPercent(SRC, 5f);
            stats.getFluxCapacity().modifyPercent(SRC, 10f);
        }
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
