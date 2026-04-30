package skills_overhaul.collective;

import java.util.HashMap;
import java.util.Map;

public final class ChargeStackTracker {

    private static final float DECAY_PER_SEC    = 0.5f;
    private static final float DETONATION_LIMIT = 15f;

    private final Map<String, Float> stacks = new HashMap<String, Float>();

    /** Adds one stack for the given attacker:target pair key. */
    public void addStack(String pairKey) {
        float current = stacks.containsKey(pairKey) ? stacks.get(pairKey) : 0f;
        stacks.put(pairKey, current + 1f);
    }

    /** Returns the integer floor of the current stack count. */
    public int getStacks(String pairKey) {
        Float v = stacks.get(pairKey);
        return v == null ? 0 : (int) Math.floor(v);
    }

    /**
     * If stacks >= 15, resets to 0 and returns true (detonation triggered).
     * Otherwise returns false.
     */
    public boolean checkAndConsumeDetonation(String pairKey) {
        Float v = stacks.get(pairKey);
        if (v != null && v >= DETONATION_LIMIT) {
            stacks.put(pairKey, 0f);
            return true;
        }
        return false;
    }

    /** Decays all pair stacks by 0.5 * amount, clamped to 0. */
    public void advance(float amount) {
        float decay = DECAY_PER_SEC * amount;
        for (Map.Entry<String, Float> entry : stacks.entrySet()) {
            float newVal = Math.max(0f, entry.getValue() - decay);
            entry.setValue(newVal);
        }
    }
}
