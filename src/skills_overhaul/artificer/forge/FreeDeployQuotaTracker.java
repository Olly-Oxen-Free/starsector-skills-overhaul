package skills_overhaul.artificer.forge;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FreeDeployQuotaTracker {
    public static final int BASE = 1;
    private static final Map<String, Integer> BONUSES = new LinkedHashMap<>();

    public static int getQuota() {
        int total = BASE;
        for (Integer v : BONUSES.values()) total += v;
        return total;
    }

    public static void registerBonus(String key, int bonus) { BONUSES.put(key, bonus); }
    public static void unregisterBonus(String key) { BONUSES.remove(key); }

    /** Load-time reset — statics survive across save loads in the same JVM. */
    public static void reset() { BONUSES.clear(); }

    public static void clearForTest() { reset(); }

    private FreeDeployQuotaTracker() {}
}
