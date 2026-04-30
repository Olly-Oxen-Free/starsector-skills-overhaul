package skills_overhaul.collective;

import java.util.HashMap;
import java.util.Map;

public final class SOFurorMachinaeTracker {

    public static final float MAX_STACKS        = 10f;
    public static final float STACK_DECAY_RATE  = 1f; // stacks per second

    private final Map<String, Float> stacks = new HashMap<String, Float>();

    public void recordHit(String shipId, float weight) {
        float current = getStacks(shipId);
        stacks.put(shipId, Math.min(MAX_STACKS, current + weight));
    }

    public void advance(String shipId, float amount) {
        float current = getStacks(shipId);
        stacks.put(shipId, Math.max(0f, current - STACK_DECAY_RATE * amount));
    }

    public float getStacks(String shipId) {
        Float s = stacks.get(shipId);
        return s != null ? s : 0f;
    }
}
