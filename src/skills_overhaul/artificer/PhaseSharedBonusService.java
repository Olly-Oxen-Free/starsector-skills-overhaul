package skills_overhaul.artificer;

public final class PhaseSharedBonusService {

    public static final float POOL_SIZE = 150f;
    public static final float FLOOR_BONUS = 0.20f;
    public static final float CAP_BONUS = 1.50f;

    public static float computeBonus(float totalPhaseDPDeployed) {
        if (totalPhaseDPDeployed <= 0f) return CAP_BONUS;
        // POOL_SIZE is in percentage points; FLOOR_BONUS/CAP_BONUS are fractions.
        float raw = (POOL_SIZE / (totalPhaseDPDeployed / 10f)) / 100f;
        if (raw < FLOOR_BONUS) return FLOOR_BONUS;
        if (raw > CAP_BONUS) return CAP_BONUS;
        return raw;
    }

    public static float computeBonusForLivePhaseFleet() {
        com.fs.starfarer.api.combat.CombatEngineAPI engine =
                com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null) return CAP_BONUS;
        float totalDP = 0f;
        for (com.fs.starfarer.api.combat.ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isAlive()) continue;
            if (!isPhaseShip(s)) continue;
            try {
                totalDP += s.getFleetMember().getDeploymentPointsCost();
            } catch (Throwable t) {
                totalDP += 20f;
            }
        }
        return computeBonus(totalDP);
    }

    private static boolean isPhaseShip(com.fs.starfarer.api.combat.ShipAPI s) {
        return s.getPhaseCloak() != null;
    }

    private PhaseSharedBonusService() {}
}
