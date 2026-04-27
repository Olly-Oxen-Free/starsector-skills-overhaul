package skills_overhaul.zealot;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.Map;

public final class RivalryService {

    public static final String K_RIVAL = "$skills.zealot.rival.factionId";
    public static final String K_LAST_DESIGNATE = "$skills.zealot.rival.lastDesignateTimestamp";
    public static final long COOLDOWN_MS = 30L * 86400L * 1000L;

    // --- Map<String,Object> overloads (for tests and MemoryWrapMap) ---

    public static String getCurrentRival(Map<String,Object> mem) {
        Object v = mem.get(K_RIVAL);
        return v instanceof String ? (String) v : null;
    }

    public static boolean canDesignate(Map<String,Object> mem, long now) {
        Object v = mem.get(K_LAST_DESIGNATE);
        if (!(v instanceof Number)) return true;
        return now - ((Number) v).longValue() >= COOLDOWN_MS;
    }

    public static void designate(Map<String,Object> mem, String factionId, long now) {
        mem.put(K_RIVAL, factionId);
        mem.put(K_LAST_DESIGNATE, now);
    }

    public static boolean isRival(Map<String,Object> mem, String factionId) {
        String r = getCurrentRival(mem);
        return r != null && r.equals(factionId);
    }

    // --- MemoryAPI overloads ---

    public static String getCurrentRival(MemoryAPI mem) {
        Object v = mem.get(K_RIVAL);
        return v instanceof String ? (String) v : null;
    }

    public static boolean canDesignate(MemoryAPI mem, long now) {
        Object v = mem.get(K_LAST_DESIGNATE);
        if (!(v instanceof Number)) return true;
        return now - ((Number) v).longValue() >= COOLDOWN_MS;
    }

    public static void designate(MemoryAPI mem, String factionId, long now) {
        mem.set(K_RIVAL, factionId);
        mem.set(K_LAST_DESIGNATE, now);
    }

    public static boolean isRival(MemoryAPI mem, String factionId) {
        String r = getCurrentRival(mem);
        return r != null && r.equals(factionId);
    }

    private RivalryService() {}
}
