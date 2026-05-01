package skills_overhaul.pilot.maneuvering;

public final class AceKillStacker {
    private static final int MAX_STACKS = 5;
    private static final float BONUS_PER_STACK = 0.04f;
    private static final float DURATION = 8f;

    private int stacks = 0;
    private float timer = 0f;
    private final float hullClassMult;

    public AceKillStacker(float hullClassMult) { this.hullClassMult = hullClassMult; }

    public void onFighterKill() { addStacks(1); }
    public void onFrigateKill() { addStacks(3); }

    private void addStacks(int n) {
        stacks = Math.min(MAX_STACKS, stacks + n);
        timer = DURATION;
    }

    public void advance(float dt) {
        if (stacks == 0) return;
        timer -= dt;
        if (timer <= 0f) { stacks = 0; timer = 0f; }
    }

    public int getStackCount() { return stacks; }
    public float getManeuverBonus() { return stacks * BONUS_PER_STACK * hullClassMult; }
}
