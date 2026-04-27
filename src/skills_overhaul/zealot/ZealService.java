package skills_overhaul.zealot;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ZealService {

    public static final int BASE_CAP = 100;
    public static final float FLAT_DECAY = 1.5f;
    public static final float DECAY_PER_STACK_PCT = 0.02f;

    public static final String K_STACKS      = "$skills.zealot.zeal.stacks";
    public static final String K_LAST_GAIN   = "$skills.zealot.zeal.lastGainDay";
    public static final String K_LAST_TICK   = "$skills.zealot.zeal.lastTickDay";
    public static final String K_CAP_BONUSES = "$skills.zealot.zeal.capBonuses";

    public static int getStacks(Map<String,Object> mem) {
        Object v = mem.get(K_STACKS);
        return v == null ? 0 : ((Number) v).intValue();
    }

    public static void addStacks(Map<String,Object> mem, int delta, int today) {
        if (delta <= 0) return;
        int cap = getCap(mem);
        int cur = getStacks(mem);
        int next = Math.min(cap, cur + delta);
        mem.put(K_STACKS, next);
        mem.put(K_LAST_GAIN, today);
    }

    public static int getCap(Map<String,Object> mem) {
        return BASE_CAP + sumCapBonuses(mem);
    }

    @SuppressWarnings("unchecked")
    public static void registerCapBonus(Map<String,Object> mem, String key, int bonus) {
        Map<String,Integer> bonuses = (Map<String,Integer>) mem.get(K_CAP_BONUSES);
        if (bonuses == null) {
            bonuses = new LinkedHashMap<>();
            mem.put(K_CAP_BONUSES, bonuses);
        }
        bonuses.put(key, bonus);
    }

    @SuppressWarnings("unchecked")
    public static void unregisterCapBonus(Map<String,Object> mem, String key) {
        Map<String,Integer> bonuses = (Map<String,Integer>) mem.get(K_CAP_BONUSES);
        if (bonuses != null) bonuses.remove(key);
    }

    @SuppressWarnings("unchecked")
    private static int sumCapBonuses(Map<String,Object> mem) {
        Map<String,Integer> bonuses = (Map<String,Integer>) mem.get(K_CAP_BONUSES);
        if (bonuses == null) return 0;
        int sum = 0;
        for (Integer v : bonuses.values()) sum += v;
        return sum;
    }

    public static void advanceDay(Map<String,Object> mem, int today, float singularityReductionPct) {
        Integer lastTick = (Integer) mem.get(K_LAST_TICK);
        if (lastTick == null) {
            // Bootstrap: treat the epoch as day 0 so first call can decay days 1..today
            lastTick = 0;
        }
        if (today - lastTick <= 0) return;

        Integer lastGain = (Integer) mem.get(K_LAST_GAIN);
        float reductionMult = 1f - Math.min(0.30f, singularityReductionPct);

        for (int day = lastTick + 1; day <= today; day++) {
            if (lastGain != null && day - lastGain.intValue() <= 1) continue;
            int stacks = getStacks(mem);
            float decay = (FLAT_DECAY + DECAY_PER_STACK_PCT * stacks) * reductionMult;
            int newStacks = Math.max(0, stacks - Math.round(decay));
            mem.put(K_STACKS, newStacks);
        }
        mem.put(K_LAST_TICK, today);
    }

    // MemoryAPI bridges
    public static int getStacks(MemoryAPI mem) {
        Object v = mem.get(K_STACKS);
        return v == null ? 0 : ((Number) v).intValue();
    }

    public static void addStacks(MemoryAPI mem, int delta, int today) {
        if (delta <= 0) return;
        int cap = getCap(mem);
        int cur = getStacks(mem);
        int next = Math.min(cap, cur + delta);
        mem.set(K_STACKS, next);
        mem.set(K_LAST_GAIN, today);
    }

    public static int getCap(MemoryAPI mem) {
        return BASE_CAP + sumCapBonuses(mem);
    }

    @SuppressWarnings("unchecked")
    private static int sumCapBonuses(MemoryAPI mem) {
        Object v = mem.get(K_CAP_BONUSES);
        if (!(v instanceof Map)) return 0;
        int sum = 0;
        for (Object n : ((Map<String,Integer>) v).values()) sum += ((Number) n).intValue();
        return sum;
    }

    private ZealService() {}
}
