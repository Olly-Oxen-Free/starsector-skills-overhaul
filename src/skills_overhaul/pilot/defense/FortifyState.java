package skills_overhaul.pilot.defense;

public final class FortifyState {
    private static final int MAX_STACKS = 5;
    private static final float DR_PER_STACK = 0.05f;
    private static final float GRACE = 0.5f;

    private int stacks = 0;
    private float timeSinceLastHit = 0f;
    private float decayAccum = 0f;

    public void onQualifyingHit() {
        stacks = Math.min(MAX_STACKS, stacks + 1);
        timeSinceLastHit = 0f;
        decayAccum = 0f;
    }

    public void advance(float dt) {
        timeSinceLastHit += dt;
        if (stacks <= 0 || timeSinceLastHit < GRACE) return;
        decayAccum += dt;
        int decay = (int) decayAccum;
        if (decay > 0) {
            stacks = Math.max(0, stacks - decay);
            decayAccum -= decay;
        }
    }

    public int getStacks() { return stacks; }
    public float getDRMult() { return 1f - stacks * DR_PER_STACK; }
    public void reset() { stacks = 0; timeSinceLastHit = 0f; decayAccum = 0f; }
}
