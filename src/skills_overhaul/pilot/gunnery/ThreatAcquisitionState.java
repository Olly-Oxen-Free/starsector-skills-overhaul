package skills_overhaul.pilot.gunnery;

public final class ThreatAcquisitionState {
    private static final int MAX_STACKS = 20;
    private static final float NO_HIT_DECAY = 10f;
    private static final float KILL_CARRY_WINDOW = 10f;

    private int stacks = 0;
    private float noHitTimer = 0f;
    private float carryTimer = 0f;
    private boolean inCarry = false;

    public void onHittingTarget(float dt) {
        noHitTimer = 0f;
        inCarry = false;
        carryTimer = 0f;
        stacks = Math.min(MAX_STACKS, stacks + (int) dt);
    }

    public void advanceNoHit(float dt) {
        if (inCarry) {
            carryTimer -= dt;
            if (carryTimer <= 0f) { stacks = 0; inCarry = false; }
        } else {
            noHitTimer += dt;
            if (noHitTimer >= NO_HIT_DECAY) stacks = 0;
        }
    }

    public void onKill() {
        inCarry = true;
        carryTimer = KILL_CARRY_WINDOW;
        noHitTimer = 0f;
    }

    public int getStacks() { return stacks; }
    public float getBonusFraction() { return stacks * 0.01f; }
    public void reset() { stacks = 0; noHitTimer = 0f; carryTimer = 0f; inCarry = false; }
}
