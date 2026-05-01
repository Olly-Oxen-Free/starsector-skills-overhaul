package skills_overhaul.pilot.systems;

public final class ReactiveSystemsTracker {
    public static final int HULL_FRIGATE   = 0;
    public static final int HULL_DESTROYER = 1;
    public static final int HULL_CRUISER   = 2;
    public static final int HULL_CAPITAL   = 3;

    private static final float PHASE_REDUCTION_RATE = 0.75f;
    private static final float PHASE_MIN_SECONDS = 3f;
    private static final float[] SHIELD_DMG_PER_SECOND = { 50f, 100f, 200f, 400f };

    private final boolean isPhaseShip;
    private final int hullClass;

    private float phaseSecondsAccum = 0f;
    private float shieldDamageAccum = 0f;
    private float pendingReduction = 0f;

    public ReactiveSystemsTracker(boolean isPhaseShip, int hullClass) {
        this.isPhaseShip = isPhaseShip;
        this.hullClass = hullClass;
    }

    public void advancePhase(float dt) {
        if (!isPhaseShip) return;
        phaseSecondsAccum += dt;
        if (phaseSecondsAccum >= PHASE_MIN_SECONDS) {
            float excess = phaseSecondsAccum - PHASE_MIN_SECONDS;
            int ticks = (int) excess;
            if (ticks > 0) {
                pendingReduction += ticks * PHASE_REDUCTION_RATE;
                phaseSecondsAccum = PHASE_MIN_SECONDS + (excess - ticks);
            }
        }
    }

    public void onPhaseExit() { phaseSecondsAccum = 0f; }

    public void onShieldDamage(float damage) {
        if (isPhaseShip) return;
        float threshold = (hullClass >= 0 && hullClass < SHIELD_DMG_PER_SECOND.length)
                ? SHIELD_DMG_PER_SECOND[hullClass] : 100f;
        shieldDamageAccum += damage;
        while (shieldDamageAccum >= threshold) {
            pendingReduction += 1f;
            shieldDamageAccum -= threshold;
        }
    }

    public float drainCooldownReduction(float dt) {
        float drained = Math.min(pendingReduction, dt);
        pendingReduction = Math.max(0f, pendingReduction - drained);
        return drained;
    }

    public void reset() { phaseSecondsAccum = 0f; shieldDamageAccum = 0f; pendingReduction = 0f; }
}
