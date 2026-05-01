package skills_overhaul.pilot.maneuvering;

public final class PerfectTimingTracker {
    private static final float THRESHOLD = 6f;

    private float noHitTimer = 0f;

    public void advance(float dt, boolean tookDamage) {
        if (tookDamage) { noHitTimer = 0f; return; }
        noHitTimer += dt;
    }

    public boolean isBuffActive() { return noHitTimer >= THRESHOLD; }
    public void reset() { noHitTimer = 0f; }
}
