package skills_overhaul.zealot;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.Map;

public final class CrusadeService {

    public static final String K_ACTIVE       = "$skills.zealot.crusade.active";
    public static final String K_RIVAL        = "$skills.zealot.crusade.rivalFactionId";
    public static final String K_DECLARED_AT  = "$skills.zealot.crusade.declaredAtMs";
    public static final String K_COOLDOWN_END = "$skills.zealot.crusade.cooldownEndMs";

    public static final long MIN_COMMITMENT_MS          = 180L * 86400L * 1000L;
    public static final long COOLDOWN_AFTER_CANCEL_MS   = 180L * 86400L * 1000L;
    public static final int  CAP_BONUS                  = 50;

    // --- Map<String,Object> overloads ---

    public static boolean isActive(Map<String,Object> mem) {
        return Boolean.TRUE.equals(mem.get(K_ACTIVE));
    }

    public static String getCrusadeRival(Map<String,Object> mem) {
        Object v = mem.get(K_RIVAL);
        return v instanceof String ? (String) v : null;
    }

    public static boolean canDeclare(Map<String,Object> mem, long now) {
        if (isActive(mem)) return false;
        Object cdEnd = mem.get(K_COOLDOWN_END);
        if (cdEnd instanceof Number) return now >= ((Number) cdEnd).longValue();
        return true;
    }

    public static void declare(Map<String,Object> mem, String rivalFactionId, long now) {
        mem.put(K_ACTIVE, true);
        mem.put(K_RIVAL, rivalFactionId);
        mem.put(K_DECLARED_AT, now);
    }

    public static boolean canManualCancel(Map<String,Object> mem, long now) {
        if (!isActive(mem)) return false;
        Object declared = mem.get(K_DECLARED_AT);
        if (!(declared instanceof Number)) return true;
        return now - ((Number) declared).longValue() >= MIN_COMMITMENT_MS;
    }

    public static void manualCancel(Map<String,Object> mem, long now) {
        mem.put(K_ACTIVE, false);
        mem.remove(K_RIVAL);
        mem.put(K_COOLDOWN_END, now + COOLDOWN_AFTER_CANCEL_MS);
        mem.put(ZealService.K_STACKS, 0);
    }

    public static void rivalDestroyed(Map<String,Object> mem) {
        mem.put(K_ACTIVE, false);
        mem.remove(K_RIVAL);
    }

    // --- MemoryAPI overloads ---

    public static boolean isActive(MemoryAPI mem) {
        return Boolean.TRUE.equals(mem.get(K_ACTIVE));
    }

    public static String getCrusadeRival(MemoryAPI mem) {
        Object v = mem.get(K_RIVAL);
        return v instanceof String ? (String) v : null;
    }

    private CrusadeService() {}
}
