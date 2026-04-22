package skills_overhaul.tree;

import java.util.*;

/**
 * Topological sort over a set of pending node IDs using prereq edges.
 * Only edges where BOTH endpoints are in the pending set are counted;
 * owned-prereqs are already satisfied facts and don't constrain ordering.
 *
 * Throws IllegalStateException on cycle (should not occur in practice —
 * pending IDs are only addable when reachable from owned-or-pending).
 */
public final class PendingPurchaseTopoSort {

    public static List<String> sort(TreeDef def, Set<String> pending) {
        // Build in-degree map + adjacency (prereq -> dependents), restricted to pending ∩ pending.
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (String id : pending) {
            inDegree.put(id, 0);
            adj.put(id, new ArrayList<String>());
        }
        for (String id : pending) {
            Node n = def.getNode(id);
            if (n == null) continue;
            for (String p : n.getPrereqs()) {
                if (pending.contains(p)) {
                    adj.get(p).add(id);
                    inDegree.put(id, inDegree.get(id) + 1);
                }
            }
        }

        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<String> out = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            out.add(id);
            for (String child : adj.get(id)) {
                int d = inDegree.get(child) - 1;
                inDegree.put(child, d);
                if (d == 0) queue.add(child);
            }
        }

        if (out.size() != pending.size()) {
            throw new IllegalStateException("cycle in pending set: " + pending);
        }
        return out;
    }

    private PendingPurchaseTopoSort() {}
}
