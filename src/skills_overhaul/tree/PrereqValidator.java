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
