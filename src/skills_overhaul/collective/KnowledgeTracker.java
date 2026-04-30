package skills_overhaul.collective;

/**
 * Tracks per-AI-core Knowledge points via a MemoryView (campaign memory).
 * Keys are namespaced per coreId to allow multiple cores to accumulate independently.
 */
public final class KnowledgeTracker {

    // [TUNE] per-event Knowledge rewards
    private static final float KNOWLEDGE_PER_KILL        = 5f;
    private static final float KNOWLEDGE_PER_BATTLE_WIN  = 10f;

    private KnowledgeTracker() {}

    private static String key(String coreId) {
        return "$so_collective.knowledge." + coreId;
    }

    public static float getKnowledge(MemoryView mem, String coreId) {
        Object v = mem.get(key(coreId));
        return v instanceof Number ? ((Number) v).floatValue() : 0f;
    }

    public static KnowledgeTier getTier(MemoryView mem, String coreId) {
        return KnowledgeTier.fromKnowledge(getKnowledge(mem, coreId));
    }

    public static void addKnowledge(MemoryView mem, String coreId, float amount) {
        float current = getKnowledge(mem, coreId);
        mem.set(key(coreId), current + amount);
    }

    public static void recordKill(MemoryView mem, String coreId) {
        addKnowledge(mem, coreId, KNOWLEDGE_PER_KILL);
    }

    public static void recordBattleWin(MemoryView mem, String coreId) {
        addKnowledge(mem, coreId, KNOWLEDGE_PER_BATTLE_WIN);
    }

    public static void reset(MemoryView mem, String coreId) {
        mem.unset(key(coreId));
    }
}
