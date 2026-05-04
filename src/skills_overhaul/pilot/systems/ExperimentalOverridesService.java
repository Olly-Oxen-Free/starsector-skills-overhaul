package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.combat.ShipAPI;

public final class ExperimentalOverridesService {

    public static boolean hasSecondaryHullmod(ShipAPI ship) {
        if (ship == null) return false;
        for (String id : ship.getVariant().getHullMods()) {
            if (id.startsWith("so_secondary_")) return true;
        }
        return false;
    }

    private ExperimentalOverridesService() {}
}
