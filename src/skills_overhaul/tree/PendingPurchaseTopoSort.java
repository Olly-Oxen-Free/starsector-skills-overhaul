package skills_overhaul.tree;

import java.util.*;

public final class PendingPurchaseTopoSort {

    public static List<String> sort(TreeDef def, Set<String> pending) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (String id : pending) {
            inDegree.put(id, 0);
            adj.put(id, new ArrayList<String>());
        }
        for (String id : pending) {
            Node n = def.getNode(id);
            if (n == null) continue;
            for (String p : n.getReferencedPrereqIds()) {
                if (pending.contains(p)) {
                    adj.get(p).add(id);
                    inDegree.put(id, inDegree.get(id) + 1);
                }
            }
        }

        // Ready nodes are taken lowest tier first (ties in pending order), so every
        // node is bought after the pending nodes its previous-tier gate relies on.
        List<String> ready = new ArrayList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) ready.add(e.getKey());
        }

        List<String> out = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.remove(lowestTier(def, ready));
            out.add(id);
            for (String child : adj.get(id)) {
                int d = inDegree.get(child) - 1;
                inDegree.put(child, d);
                if (d == 0) ready.add(child);
            }
        }

        if (out.size() != pending.size()) {
            throw new IllegalStateException("cycle in pending set: " + pending);
        }
        return out;
    }

    private static int lowestTier(TreeDef def, List<String> ids) {
        int best = 0;
        for (int i = 1; i < ids.size(); i++) {
            if (tierOf(def, ids.get(i)) < tierOf(def, ids.get(best))) best = i;
        }
        return best;
    }

    private static int tierOf(TreeDef def, String id) {
        Node n = def.getNode(id);
        return n == null ? 0 : n.getTier();
    }

    private PendingPurchaseTopoSort() {}
}
