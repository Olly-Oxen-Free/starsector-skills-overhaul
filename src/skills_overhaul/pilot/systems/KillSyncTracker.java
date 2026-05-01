package skills_overhaul.pilot.systems;

public final class KillSyncTracker {
    public static final int CLASS_FRIGATE = 0;
    public static final int CLASS_CRUISER = 1;
    public static final int CLASS_CAPITAL = 2;

    private static final float BASE_REDUCTION = 4f;
    private static final float[] CLASS_SCALAR = { 1.00f, 0.66f, 0.33f };

    private float pendingReduction = 0f;

    public float computeReduction(float killedDP, float flagshipDP, int killClass) {
        if (flagshipDP <= 0f) return 0f;
        float ratio = Math.min(2.0f, Math.max(0.3f, killedDP / flagshipDP));
        float scalar = (killClass >= 0 && killClass < CLASS_SCALAR.length)
                ? CLASS_SCALAR[killClass] : 1.0f;
        return BASE_REDUCTION * ratio * scalar;
    }

    public void applyReduction(float amount) { pendingReduction += amount; }

    public void onSecondaryActivation() { applyReduction(1.5f); }

    public float drain(float dt) {
        float drained = Math.min(pendingReduction, dt);
        pendingReduction = Math.max(0f, pendingReduction - dt);
        return drained;
    }

    public float getPendingReduction() { return pendingReduction; }
    public void reset() { pendingReduction = 0f; }
}
