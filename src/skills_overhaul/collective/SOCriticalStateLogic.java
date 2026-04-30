package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class SOCriticalStateLogic {

    private final SOCriticalStateTracker tracker = new SOCriticalStateTracker();

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_critical_state")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAICored(ship)) continue;

            String id = ship.getId();
            if (!tracker.hasTriggered(id) && ship.getHullLevel() < 0.5f) {
                tracker.markTriggered(id);
                ship.getMutableStats().getTimeMult().modifyMult("so_timescale", 1.3f);
            }
        }
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
