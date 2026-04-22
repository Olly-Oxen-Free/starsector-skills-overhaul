package skills_overhaul.tree;

import skills_overhaul.state.SkillMemory;

import java.util.Map;
import java.util.Set;

public final class PrereqValidator {

    public static boolean canPurchase(TreeDef def, Map<String,Object> mem, String nodeId) {
        Node n = def.getNode(nodeId);
        if (n == null) return false;
        if (Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(nodeId)))) return false;
        for (String p : n.getPrereqs()) {
            if (!Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(p)))) return false;
        }
        return true;
    }

    /**
     * Prereq check where a prereq is satisfied if it is EITHER owned OR in the pending set.
     * Used by the UI to decide whether a node becomes unlockable when the player has
     * queued (but not yet confirmed) upstream purchases.
     *
     * Rejects the node if it is itself already owned or already pending.
     */
    public static boolean canPurchaseWithPending(TreeDef def, Map<String,Object> mem,
                                                 Set<String> pending, String nodeId) {
        Node n = def.getNode(nodeId);
        if (n == null) return false;
        if (Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(nodeId)))) return false;
        if (pending.contains(nodeId)) return false;
        for (String p : n.getPrereqs()) {
            boolean owned = Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(p)));
            if (!owned && !pending.contains(p)) return false;
        }
        return true;
    }
}
