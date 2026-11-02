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

    /**
     * Previous-tier gate: a node above tier 0 needs at least one node of the tier
     * directly below it in the same tree, owned or pending.
     */
    public static boolean tierGateSatisfied(TreeDef def, Map<String,Object> mem,
                                            Set<String> pending, String nodeId) {
        Node n = def.getNode(nodeId);
        if (n == null) return false;
        if (n.getTier() <= 0) return true;
        int prevTier = n.getTier() - 1;
        for (Node o : def.getNodes()) {
            if (o.getTier() != prevTier) continue;
            if (Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(o.getId())))) return true;
            if (pending.contains(o.getId())) return true;
        }
        return false;
    }

    /**
     * Removes from pending every node of this tree that no longer passes its prereqs
     * or the tier gate against owned + the rest of pending, until stable.
     * Returns the removed ids.
     */
    public static Set<String> prunePending(TreeDef def, Map<String,Object> mem, Set<String> pending) {
        Set<String> removed = new LinkedHashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String id : new ArrayList<>(pending)) {
                if (def.getNode(id) == null) continue;
                Set<String> others = new LinkedHashSet<>(pending);
                others.remove(id);
                if (!canPurchaseWithPending(def, mem, others, id)
                        || !tierGateSatisfied(def, mem, others, id)) {
                    pending.remove(id);
                    removed.add(id);
                    changed = true;
                }
            }
        }
        return removed;
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
