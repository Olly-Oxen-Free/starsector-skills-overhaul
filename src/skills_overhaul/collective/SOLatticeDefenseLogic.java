package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class SOLatticeDefenseLogic {

    private static final String SRC = "so_lattice_defense";

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_lattice_defense")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAICored(ship)) continue;
            if (ship.getShield() == null) continue;

            FluxTrackerAPI flux = ship.getFluxTracker();
            MutableShipStatsAPI stats = ship.getMutableStats();

            if (flux.getMaxFlux() <= 0f) continue;

            float hardFluxFraction = flux.getHardFlux() / flux.getMaxFlux();
            float bonus = Math.min(hardFluxFraction, 0.30f);

            if (bonus <= 0f) {
                stats.getShieldDamageTakenMult().unmodify(SRC);
            } else {
                // Lower damage-taken mult = better efficiency (spec: +30% efficiency at 100% hard flux)
                stats.getShieldDamageTakenMult().modifyMult(SRC, 1f - bonus);
            }
        }
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
