package skills_overhaul.artificer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class ECMTracker {

    public static float ratingForFleet(int combatShipCount, int automatedShipCount) {
        return 0.015f * combatShipCount + 0.030f * automatedShipCount;
    }

    public static float advantage(float playerRating, float enemyRating) {
        return playerRating - enemyRating;
    }

    public static float enemyWeaponRangePenaltyMult(float advantagePct) {
        float reduction = Math.min(0.10f, Math.max(0f, advantagePct));
        return 1f - reduction;
    }

    public static float getPlayerECMRatingLive() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return 0f;
        int combat = 0, automated = 0;
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isAlive()) continue;
            combat++;
            if (isAutomated(s)) automated++;
        }
        return ratingForFleet(combat, automated);
    }

    public static float getEnemyECMRatingLive() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return 0f;
        int combat = 0, automated = 0;
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 1 || !s.isAlive()) continue;
            combat++;
            if (isAutomated(s)) automated++;
        }
        return ratingForFleet(combat, automated);
    }

    private static boolean isAutomated(ShipAPI s) {
        // Vanilla automated hulls carry the built-in "automated" hullmod, not a hullspec tag.
        return com.fs.starfarer.api.util.Misc.isAutomated(s);
    }

    private ECMTracker() {}
}
