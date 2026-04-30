package skills_overhaul.pilot.defense;

public final class TargeState {
    private static final float MAX_BONUS = 0.50f;
    private static final float REGEN_PER_SEC = 0.001f;

    private float totalHullLostFraction = 0f;

    public void recordHullLost(float fraction) {
        totalHullLostFraction = Math.min(MAX_BONUS, totalHullLostFraction + fraction);
    }

    public float getWeaponDamageMult() { return 1f + totalHullLostFraction; }
    public float getRegenPerSecondFraction() { return REGEN_PER_SEC; }
    public void reset() { totalHullLostFraction = 0f; }
}
