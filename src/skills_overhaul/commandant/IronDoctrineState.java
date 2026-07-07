package skills_overhaul.commandant;

public final class IronDoctrineState {

    public static final float TOTAL_CONTROL_THRESHOLD = 10f;
    /** Combat seconds that Marching Advance persists after the fleet loses objective majority. */
    public static final float MARCHING_GRACE_SECONDS = 10f;

    private float consecutiveAllHeldSeconds = 0f;
    private float marchingGraceRemaining = 0f;

    public float entrenchedMult(int objectivesHeld) {
        int eff = Math.min(5, Math.max(0, objectivesHeld));
        return 1f + 0.05f * eff;
    }

    public boolean marchingActive(int playerHeld, int enemyHeld, boolean inGrace) {
        if (playerHeld > enemyHeld) return true;
        return inGrace;
    }

    public void tickConsecutiveAllHeld(float dt, boolean allHeldNow) {
        if (allHeldNow) {
            consecutiveAllHeldSeconds += dt;
        } else {
            consecutiveAllHeldSeconds = 0f;
        }
    }

    public float consecutiveAllHeldSeconds() { return consecutiveAllHeldSeconds; }
    public boolean totalControlActive() { return consecutiveAllHeldSeconds >= TOTAL_CONTROL_THRESHOLD; }

    public void tickMarchingGrace(float dt, boolean majorityHeld) {
        if (majorityHeld) {
            marchingGraceRemaining = MARCHING_GRACE_SECONDS;
        } else {
            marchingGraceRemaining = Math.max(0f, marchingGraceRemaining - dt);
        }
    }

    public boolean isInMarchingGrace() { return marchingGraceRemaining > 0f; }

    public void resetForBattle() {
        consecutiveAllHeldSeconds = 0f;
        marchingGraceRemaining = 0f;
    }
}
