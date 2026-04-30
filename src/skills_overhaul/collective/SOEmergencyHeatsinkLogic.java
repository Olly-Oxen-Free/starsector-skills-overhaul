package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SOEmergencyHeatsinkLogic {

    private static final String SRC_VENT  = "so_vent_rate";
    private static final String SRC_SPEED = "so_heatsink_speed";
    private static final float MIN_GUARD_TIME = 0.5f;

    private final Set<String> usedHeatsink = new HashSet<String>();
    private final Map<String, Boolean> wasVenting = new HashMap<String, Boolean>();
    private final Map<String, Float> ventTimer = new HashMap<String, Float>();

    public void advance(CombatEngineAPI engine, float amount) {
        if (!CollectiveActiveNodes.isActive("so_collective_emergency_heatsink")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAICored(ship)) continue;

            String id = ship.getId();
            FluxTrackerAPI flux = ship.getFluxTracker();
            MutableShipStatsAPI stats = ship.getMutableStats();

            // Trigger heatsink when flux hits 100% and ship isn't already overloaded/venting
            if (!usedHeatsink.contains(id)
                    && flux.getFluxLevel() >= 0.999f
                    && !flux.isOverloaded()
                    && !flux.isVenting()) {
                usedHeatsink.add(id);
                flux.setHardFlux(0f);
                flux.ventFlux();
                stats.getVentRateMult().modifyPercent(SRC_VENT, 100f);
                stats.getMaxSpeed().modifyPercent(SRC_SPEED, 20f);
                wasVenting.put(id, Boolean.TRUE);
                ventTimer.put(id, 0f);
            }

            // Remove buffs when venting ends (after minimum guard time)
            Boolean venting = wasVenting.getOrDefault(id, Boolean.FALSE);
            if (venting) {
                float elapsed = ventTimer.getOrDefault(id, 0f) + amount;
                ventTimer.put(id, elapsed);
                if (elapsed >= MIN_GUARD_TIME && !flux.isVenting()) {
                    stats.getVentRateMult().unmodify(SRC_VENT);
                    stats.getMaxSpeed().unmodify(SRC_SPEED);
                    wasVenting.put(id, Boolean.FALSE);
                    ventTimer.remove(id);
                }
            }
        }
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
