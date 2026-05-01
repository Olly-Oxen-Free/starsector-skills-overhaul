package skills_overhaul.pilot.systems;

public final class TargetPainterState {
    private static final float DAMAGE_THRESHOLD = 150f;
    private static final float STACK_RATE_LIMIT = 1f;
    private static final int MAX_STACKS = 5;
    private static final float DEBUFF_PER_STACK = 0.03f;

    private float damageAccum = 0f;
    private float lastStackTime = -999f;
    private int stacks = 0;

    public void onShieldDamage(float damage, float gameTime) {
        damageAccum += damage;
        if (damageAccum >= DAMAGE_THRESHOLD && (gameTime - lastStackTime) >= STACK_RATE_LIMIT) {
            if (stacks < MAX_STACKS) stacks++;
            lastStackTime = gameTime;
            damageAccum = 0f;
        }
    }

    public void onTargetSwitch() {
        stacks = 0; damageAccum = 0f; lastStackTime = -999f;
    }

    public int getStacks() { return stacks; }
    public float getShieldEfficiencyDebuff() { return stacks * DEBUFF_PER_STACK; }
    public void reset() { stacks = 0; damageAccum = 0f; lastStackTime = -999f; }
}
