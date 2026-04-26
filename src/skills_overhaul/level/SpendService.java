package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.Map;

public final class SpendService {
    public static final String POOL_UNALLOC = "$skills.points.universal_migrated_unallocated";
    // pool locks to the first tree that spends from it
    public static final String POOL_LOCK_KEY = "$skills.points.universal_migrated_locked_to";

    public static class Result {
        public boolean success;
        public int fromTreeLocked;
        public int fromFreeUni;
        public int fromMigrationToPilot;
        public int fromMigrationToCommander;
        public int fromMigrationToAdmin;
        public String reason = "";
    }

    public static Result spend(Map<String,Object> mem, Tree tree, String nodeId, int cost) {
        Result r = new Result();

        int treePts = SkillMemory.getPoints(mem, tree);
        float freeUni = f(mem.get(SkillMemory.freeUniKey()));
        float migLocked = f(mem.get(SkillMemory.migUniKey(tree)));
        float migPool = f(mem.get(POOL_UNALLOC));

        // Check whether the unallocated migration pool is locked to a different tree
        String lockedTo = (String) mem.get(POOL_LOCK_KEY);
        boolean canUsePool = (lockedTo == null || lockedTo.equals(tree.name()));

        int remaining = cost;

        // 1. Tree-locked first
        int takeTL = Math.min(remaining, treePts);
        r.fromTreeLocked = takeTL;
        remaining -= takeTL;

        // 2. Tree-locked migration universal (already-locked for this tree)
        int takeML = (int) Math.min(remaining, migLocked);
        addToResult(r, tree, takeML);
        remaining -= takeML;

        // 3. Unallocated migration universal (will lock to this tree on commit)
        int takePool = canUsePool ? (int) Math.min(remaining, migPool) : 0;
        addToResult(r, tree, takePool);
        remaining -= takePool;

        // 4. Free universal
        int takeFU = (int) Math.min(remaining, freeUni);
        r.fromFreeUni = takeFU;
        remaining -= takeFU;

        if (remaining > 0) {
            r.reason = "not enough points (short " + remaining + ")";
            r.success = false;
            return r;
        }

        // Commit
        SkillMemory.setPoints(mem, tree, treePts - takeTL);
        // Pool points lock to this tree; add them to the tree-locked migration key
        mem.put(SkillMemory.migUniKey(tree), migLocked - takeML + takePool);
        mem.put(POOL_UNALLOC, migPool - takePool);
        mem.put(SkillMemory.freeUniKey(), freeUni - takeFU);
        mem.put(SkillMemory.purchasedKey(nodeId), true);
        if (takePool > 0 && lockedTo == null) {
            mem.put(POOL_LOCK_KEY, tree.name());
        }

        r.success = true;
        return r;
    }

    public static Result refund(Map<String,Object> mem, Tree tree, String nodeId, int cost,
                                int fromTreeLocked, int fromFreeUni, int fromMigration) {
        Result r = new Result();
        SkillMemory.addPoints(mem, tree, fromTreeLocked);
        mem.put(SkillMemory.freeUniKey(), f(mem.get(SkillMemory.freeUniKey())) + fromFreeUni);
        // Return migration points to the unallocated pool (un-locking them from this tree)
        mem.put(SkillMemory.migUniKey(tree), f(mem.get(SkillMemory.migUniKey(tree))) - fromMigration);
        mem.put(POOL_UNALLOC, f(mem.get(POOL_UNALLOC)) + fromMigration);
        mem.remove(SkillMemory.purchasedKey(nodeId));

        // Clear pool lock if this tree's locked-migration count drops to 0
        float newMigLocked = f(mem.get(SkillMemory.migUniKey(tree)));
        String lockedTo = (String) mem.get(POOL_LOCK_KEY);
        if (newMigLocked == 0f && tree.name().equals(lockedTo)) {
            mem.remove(POOL_LOCK_KEY);
        }

        r.success = true;
        return r;
    }

    private static float f(Object v) { return v == null ? 0f : ((Number) v).floatValue(); }

    private static void addToResult(Result r, Tree tree, int n) {
        switch (tree) {
            case PILOT:      r.fromMigrationToPilot      += n; break;
            case COMMANDER:  r.fromMigrationToCommander  += n; break;
            case ADMIN:      r.fromMigrationToAdmin      += n; break;
        }
    }
}
