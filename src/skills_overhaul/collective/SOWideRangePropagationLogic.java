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

            // Re-apply every frame. modifyPercent keyed by the fixed SRC id is
            // idempotent, so this is a no-op for already-buffed fighters while
            // respawned fighters (which reuse slot ids) still get re-buffed —
            // the old permanent seen-set gate starved them.
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
