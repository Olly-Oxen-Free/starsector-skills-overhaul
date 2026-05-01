package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class ExperimentalOverridesService {
    public static void onKeyPressed(ShipAPI ship, CombatEngineAPI engine) { /* stub — Plan D */ }
    public static boolean hasSecondaryHullmod(ShipAPI ship) { return false; }

    private ExperimentalOverridesService() {}
}
