package skills_overhaul.corsair;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.Map;

/**
 * Day-bucket ring (int[30]) of Infamy stacks. Each slot holds the stacks recorded on a given day;
 * sum across all 30 slots is the current Infamy total. Old days rotate out automatically as the
 * in-game day advances.
 *
 * Pure logic over a Map<String,Object>; MemoryAPI overloads bridge to the live game.
 */
public final class InfamyService {

    public static final int WINDOW_DAYS = 30;

    public static final String K_BUCKETS  = "$skills.corsair.infamy.buckets";
    public static final String K_CURSOR   = "$skills.corsair.infamy.cursor";
    public static final String K_LAST_DAY = "$skills.corsair.infamy.lastDay";

    /** Roll the ring forward to `today` (zeroing newly-rotated-out slots), then return the int[30]. */
    public static int[] rollTo(Map<String,Object> mem, int today) {
        int[] buckets = (int[]) mem.get(K_BUCKETS);
        if (buckets == null) {
            buckets = new int[WINDOW_DAYS];
            mem.put(K_BUCKETS, buckets);
            mem.put(K_CURSOR, 0);
            mem.put(K_LAST_DAY, today);
            return buckets;
        }
        int cursor = ((Number) mem.get(K_CURSOR)).intValue();
        int lastDay = ((Number) mem.get(K_LAST_DAY)).intValue();
        int gap = today - lastDay;
        if (gap <= 0) return buckets;
        if (gap >= WINDOW_DAYS) {
            for (int i = 0; i < WINDOW_DAYS; i++) buckets[i] = 0;
            cursor = 0;
        } else {
            for (int i = 0; i < gap; i++) {
                cursor = (cursor + 1) % WINDOW_DAYS;
                buckets[cursor] = 0;
            }
        }
        mem.put(K_CURSOR, cursor);
        mem.put(K_LAST_DAY, today);
        return buckets;
    }

    public static void recordStacks(Map<String,Object> mem, int today, int stacks) {
        if (stacks <= 0) return;
        int[] buckets = rollTo(mem, today);
        int cursor = ((Number) mem.get(K_CURSOR)).intValue();
        buckets[cursor] += stacks;
    }

    public static int getInfamy(Map<String,Object> mem, int today) {
        int[] buckets = rollTo(mem, today);
        int sum = 0;
        for (int v : buckets) sum += v;
        return sum;
    }

    public static InfamyTier getTier(Map<String,Object> mem, int today) {
        return InfamyTier.fromInfamy(getInfamy(mem, today));
    }

    public static boolean tierActive(Map<String,Object> mem, int today, InfamyTier tier) {
        return getInfamy(mem, today) >= tier.threshold;
    }

    // ---------- MemoryAPI bridges ----------
    public static int getInfamy(MemoryAPI mem, int today) {
        return getInfamy(asMap(mem), today);
    }
    public static void recordStacks(MemoryAPI mem, int today, int stacks) {
        recordStacks(asMap(mem), today, stacks);
    }
    public static InfamyTier getTier(MemoryAPI mem, int today) {
        return InfamyTier.fromInfamy(getInfamy(mem, today));
    }
    public static boolean tierActive(MemoryAPI mem, int today, InfamyTier tier) {
        return getInfamy(mem, today) >= tier.threshold;
    }

    /** Map wrapper that write-throughs to MemoryAPI on put(). */
    private static Map<String,Object> asMap(MemoryAPI mem) {
        java.util.HashMap<String,Object> seed = new java.util.HashMap<>();
        Object b = mem.get(K_BUCKETS);
        if (b instanceof int[]) seed.put(K_BUCKETS, b);
        Object c = mem.get(K_CURSOR);
        if (c instanceof Number) seed.put(K_CURSOR, ((Number) c).intValue());
        Object l = mem.get(K_LAST_DAY);
        if (l instanceof Number) seed.put(K_LAST_DAY, ((Number) l).intValue());
        return new MemoryWriteThroughMap(seed, mem);
    }

    private static final class MemoryWriteThroughMap extends java.util.HashMap<String,Object> {
        private final MemoryAPI mem;
        MemoryWriteThroughMap(java.util.HashMap<String,Object> seed, MemoryAPI mem) {
            super(seed);
            this.mem = mem;
        }
        @Override public Object put(String key, Object value) {
            mem.set(key, value);
            return super.put(key, value);
        }
    }

    private InfamyService() {}
}
