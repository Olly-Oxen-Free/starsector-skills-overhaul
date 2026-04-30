package skills_overhaul.collective;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks per-faction engagement stacks for Combat Memory (T1).
 * Two modes:
 * - Instance mode (test use): HashMap-backed, ephemeral.
 * - Static/persistent mode: MemoryView-backed, persists across battles.
 *   Call recordEngagement(MemoryView, factionId) from CollectiveBattleEndListener.
 *   Read stacks via getStacks(MemoryView, factionId) in CollectiveCombatPlugin.
 */
public final class CombatMemoryTracker {

    private static final int MAX_STACKS = 3;
    private static final float DMG_PER_STACK = 5f;   // [TUNE]
    private static final float RES_PER_STACK = 5f;   // [TUNE]

    private static final String STACKS_PREFIX   = "$so_collective.memory.stacks.";
    private static final String LAST_FACTION_KEY = "$so_collective.memory.last_faction";

    // ── Persistent (MemoryView-backed) API ────────────────────────────────────

    /** Call once per battle won from CollectiveBattleEndListener with the primary enemy faction. */
    public static void recordEngagement(MemoryView mem, String factionId) {
        Object lastObj = mem.get(LAST_FACTION_KEY);
        String last = lastObj instanceof String ? (String) lastObj : null;
        if (last != null && !factionId.equals(last)) {
            // Switched faction — zero out previous faction's stacks
            mem.set(STACKS_PREFIX + last, 0);
        }
        mem.set(LAST_FACTION_KEY, factionId);
        int current = getStacks(mem, factionId);
        mem.set(STACKS_PREFIX + factionId, Math.min(MAX_STACKS, current + 1));
    }

    public static int getStacks(MemoryView mem, String factionId) {
        Object v = mem.get(STACKS_PREFIX + factionId);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    public static float getDamageBonusPct(MemoryView mem, String factionId) {
        return getStacks(mem, factionId) * DMG_PER_STACK;
    }

    public static float getResistBonusPct(MemoryView mem, String factionId) {
        return getStacks(mem, factionId) * RES_PER_STACK;
    }

    // ── Instance API (used by tests only) ─────────────────────────────────────

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

    public float getDamageBonus(String factionId) { return getStacks(factionId) * DMG_PER_STACK; }
    public float getResistBonus(String factionId) { return getStacks(factionId) * RES_PER_STACK; }

    public void clear() { factionStacks.clear(); lastFaction = null; }
}
