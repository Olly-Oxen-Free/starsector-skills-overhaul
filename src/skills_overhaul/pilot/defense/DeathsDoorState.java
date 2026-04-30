package skills_overhaul.pilot.defense;

public final class DeathsDoorState {
    private static final float HULL_THRESHOLD = 0.10f;
    private static final float WINDOW = 4f;

    private boolean used = false;
    private float windowRemaining = 0f;

    public boolean tryTrigger(float hullFraction) {
        if (used || hullFraction >= HULL_THRESHOLD) return false;
        used = true;
        windowRemaining = WINDOW;
        return true;
    }

    public void advance(float dt) {
        if (windowRemaining > 0f) windowRemaining = Math.max(0f, windowRemaining - dt);
    }

    public boolean isImmune() { return windowRemaining > 0f; }
    public boolean isUsed() { return used; }
    public void reset() { used = false; windowRemaining = 0f; }
}
