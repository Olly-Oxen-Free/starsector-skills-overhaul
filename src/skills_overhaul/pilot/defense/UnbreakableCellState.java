package skills_overhaul.pilot.defense;

/**
 * Per-armor-cell state for Unbreakable (T4): floor clamp, 8s block, 15s cooldown.
 * Pure Java so the clamp and timers can be tested without the game.
 */
public final class UnbreakableCellState {

    static final float BLOCK_DURATION = 8f;
    static final float COOLDOWN_DURATION = 15f;
    static final float FLOOR = 0.50f;

    private float blockRemaining = 0f;
    private float cooldownRemaining = 0f;

    /**
     * Returns the damage multiplier to apply to a hit on this cell (1 = no clamp).
     * The result is a factor for DamageAPI.getModifier(), whose base value is 1.
     */
    public float onHit(float currCell, float maxCell, float dmgValue) {
        if (cooldownRemaining > 0f) return 1f;  // can't re-trigger

        float floorVal = maxCell * FLOOR;
        if (currCell <= floorVal) return 1f;  // already below floor — no protection

        // If incoming damage would drop the cell below floor, clamp it.
        float projectedCell = currCell - dmgValue;
        if (projectedCell < floorVal) {
            float allowed = currCell - floorVal;
            // Don't restart a running block: sustained fire must still reach the cooldown.
            if (blockRemaining == 0f) startBlock();
            return Math.max(0f, allowed / dmgValue);
        }
        return 1f;
    }

    void startBlock() {
        blockRemaining = BLOCK_DURATION;
    }

    public void advance(float dt) {
        if (blockRemaining > 0f) {
            blockRemaining = Math.max(0f, blockRemaining - dt);
            if (blockRemaining == 0f) cooldownRemaining = COOLDOWN_DURATION;
        } else if (cooldownRemaining > 0f) {
            cooldownRemaining = Math.max(0f, cooldownRemaining - dt);
        }
    }

    public boolean isOnCooldown() { return cooldownRemaining > 0f; }
}
