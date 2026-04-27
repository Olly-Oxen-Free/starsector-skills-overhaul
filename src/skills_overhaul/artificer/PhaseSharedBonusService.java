package skills_overhaul.artificer;

public final class PhaseSharedBonusService {

    public static final float POOL_SIZE = 150f;
    public static final float FLOOR_BONUS = 0.20f;
    public static final float CAP_BONUS = 1.50f;

    public static float computeBonus(float totalPhaseDPDeployed) {
        if (totalPhaseDPDeployed <= 0f) return CAP_BONUS;
        float raw = POOL_SIZE / (totalPhaseDPDeployed / 10f);
        // raw is in % units (e.g. raw=30 means 30%, 150 = 150%). Convert to multiplier (1.30) — wait
        // re-read: spec says "per-ship bonus = 150 / (totalDP/10)". And "floor 20%, cap 150%".
        // So raw is already in "% units" (30 = 30%, 150 = 150%). Convert to multiplier: bonus/100.
        // But 1.50 cap is 150%, so cap_bonus 1.50f represents 150% — then a 30 raw = 30% = 0.30 mult.
        // The test asserts midBonus=0.50f for totalDP=3000 → raw=1500/3000=0.5. So raw is already in
        // multiplier form (0.5 = 50%). The math line above gives raw=0.5 directly. Floor 0.20, cap 1.50.
        // OK math is correct as-is.
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
