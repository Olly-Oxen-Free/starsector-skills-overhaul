package skills_overhaul.collective;

import java.util.HashMap;
import java.util.Map;

public final class CombatMemoryTracker {

    private static final int MAX_STACKS = 3;

    private final Map<String, Integer> factionStacks = new HashMap<String, Integer>();
    private String lastFaction = null;

    public void recordEngagement(String factionId) {
        if (lastFaction != null && !factionId.equals(lastFaction)) {
            factionStacks.clear();
        }
        lastFaction = factionId;
        int current = factionStacks.containsKey(factionId) ? factionStacks.get(factionId) : 0;
        factionStacks.put(factionId, Math.min(MAX_STACKS, current + 1));
    }

    public int getStacks(String factionId) {
        return factionStacks.containsKey(factionId) ? factionStacks.get(factionId) : 0;
    }

    /** Damage bonus % for current stacks. */
    public float getDamageBonus(String factionId) { return getStacks(factionId) * 5f; } // [TUNE]

    /** Damage resistance bonus % for current stacks. */
    public float getResistBonus(String factionId) { return getStacks(factionId) * 5f; } // [TUNE]

    public void clear() { factionStacks.clear(); lastFaction = null; }
}
