package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.*;

public final class RespecService {
    public static final String RECORD_PREFIX = "$skills.spend_record.";

    public static class Record {
        public int treeLocked, freeUni, migration;
    }

    public static class Result {
        public int storyPointsCharged;
    }

    public static void recordPurchase(Map<String,Object> mem, String nodeId,
                                      int treeLocked, int freeUni, int migration) {
        mem.put(RECORD_PREFIX + nodeId + ".tl", (float) treeLocked);
        mem.put(RECORD_PREFIX + nodeId + ".fu", (float) freeUni);
        mem.put(RECORD_PREFIX + nodeId + ".mg", (float) migration);
    }

    /** Story points a respec of {@code tree} costs: 0 until {@code respec_used} is set for it, then 1. */
    public static int costFor(Map<String,Object> snapshot, Tree tree) {
        return Boolean.TRUE.equals(snapshot.get(SkillMemory.respecUsedKey(tree))) ? 1 : 0;
    }

    public static Result respecTree(Map<String,Object> mem, Tree tree) {
        Result r = new Result();
        r.storyPointsCharged = costFor(mem, tree);
        boolean firstFree = r.storyPointsCharged == 0;

        int totalTL = 0, totalFU = 0, totalMig = 0;

        // Caller (RespecRunner) pre-filters the snapshot map to only this
        // tree's nodes. We process every record key present. Previous
        // implementation filtered via Tree.nodeIdPrefix (so_p_/so_c_/so_a_)
        // which never matched real node IDs (so_maneuver_*, commander_*,
        // admin_*) — every respec silently refunded zero points.
        for (String key : new HashSet<String>(keysWithPrefix(mem, RECORD_PREFIX))) {
            if (!key.endsWith(".tl")) continue;
            String nodeId = key.substring(RECORD_PREFIX.length(), key.length() - 3);

            int tl = intOf(mem.remove(key));
            int fu = intOf(mem.remove(RECORD_PREFIX + nodeId + ".fu"));
            int mg = intOf(mem.remove(RECORD_PREFIX + nodeId + ".mg"));
            totalTL += tl; totalFU += fu; totalMig += mg;

            mem.remove(SkillMemory.purchasedKey(nodeId));
        }

        SkillMemory.addPoints(mem, tree, totalTL);
        mem.put(SkillMemory.freeUniKey(), f(mem.get(SkillMemory.freeUniKey())) + totalFU);
        mem.put(SkillMemory.migUniKey(tree), f(mem.get(SkillMemory.migUniKey(tree))) + totalMig);

        if (firstFree) mem.put(SkillMemory.respecUsedKey(tree), true);

        return r;
    }

    private static int   intOf(Object v) { return v == null ? 0 : (int) ((Number)v).floatValue(); }
    private static float f(Object v)     { return v == null ? 0f : ((Number)v).floatValue(); }

    private static Set<String> keysWithPrefix(Map<String,Object> mem, String prefix) {
        Set<String> out = new HashSet<String>();
        for (String k : mem.keySet()) if (k.startsWith(prefix)) out.add(k);
        return out;
    }
}
