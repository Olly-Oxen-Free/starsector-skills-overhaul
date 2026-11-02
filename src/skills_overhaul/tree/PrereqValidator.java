package skills_overhaul.tree;

import skills_overhaul.state.SkillMemory;

import java.util.*;

public final class PrereqValidator {

    public static boolean canPurchase(TreeDef def, Map<String,Object> mem, String nodeId) {
        Node n = def.getNode(nodeId);
        if (n == null) return false;
        if (Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(nodeId)))) return false;
        return Prereq.allSatisfied(n.getClauses(), ownedSet(def, mem));
    }

    public static boolean canPurchaseWithPending(TreeDef def, Map<String,Object> mem,
                                                 Set<String> pending, String nodeId) {
        Node n = def.getNode(nodeId);
        if (n == null) return false;
        if (Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(nodeId)))) return false;
        if (pending.contains(nodeId)) return false;
        Set<String> effective = ownedSet(def, mem);
        effective.addAll(pending);
        return Prereq.allSatisfied(n.getClauses(), effective);
    }

    /** Outcome of {@link #check}: OK, or why the node cannot be taken. */
    public enum Check { OK, UNKNOWN, PREREQ, TIER }

    /**
     * Reason-returning prereq check with the tier gate. Ownership of {@code nodeId}
     * itself is not checked: callers decide what OWNED or already-pending means.
     * Order: UNKNOWN (no such node in {@code def}), PREREQ (All/Any/AtLeast clauses
     * against owned + pending), TIER (y &gt; 0 needs an owned or pending node in tier
     * y-1 of the same {@code def}), else OK.
     */
    public static Check check(TreeDef def, Map<String,Object> mem,
                              Set<String> pending, String nodeId) {
        Node n = def.getNode(nodeId);
        if (n == null) return Check.UNKNOWN;
        Set<String> effective = ownedSet(def, mem);
        if (pending != null) effective.addAll(pending);
        if (!Prereq.allSatisfied(n.getClauses(), effective)) return Check.PREREQ;
        if (!hasAnyInPreviousTier(def, n.getY(), effective)) return Check.TIER;
        return Check.OK;
    }

    private static boolean hasAnyInPreviousTier(TreeDef def, int tier, Set<String> effective) {
        if (tier <= 0) return true;
        int prevTier = tier - 1;
        for (Node n : def.getNodes()) {
            if (n.getY() == prevTier && effective.contains(n.getId())) return true;
        }
        return false;
    }

    private static Set<String> ownedSet(TreeDef def, Map<String,Object> mem) {
        Set<String> out = new LinkedHashSet<>();
        for (Node n : def.getNodes()) {
            if (Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(n.getId())))) {
                out.add(n.getId());
            }
        }
        return out;
    }

    private PrereqValidator() {}
}
