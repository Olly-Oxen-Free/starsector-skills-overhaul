package skills_overhaul.tree;

import org.json.JSONObject;

import java.util.*;

public class PendingPurchaseTopoSortTest {
    public static void main(String[] args) throws Exception {
        // r0 -> c1 -> c2 (chain)
        String json = "{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"r0\",\"name\":\"R0\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"c1\",\"name\":\"C1\",\"x\":2,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"r0\"]}]}," +
                "{\"id\":\"c2\",\"name\":\"C2\",\"x\":2,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"c1\"]}]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));

        Set<String> pending = new LinkedHashSet<>();
        pending.add("c2"); pending.add("r0"); pending.add("c1");
        List<String> order = PendingPurchaseTopoSort.sort(def, pending);
        assert order.indexOf("r0") < order.indexOf("c1");
        assert order.indexOf("c1") < order.indexOf("c2");

        // Pending containing only c1 (r0 owned, not pending): no inter-pending edge from r0,
        // c1 has in-degree 0 in the pending sub-graph.
        Set<String> pending2 = new LinkedHashSet<>();
        pending2.add("c1");
        List<String> order2 = PendingPurchaseTopoSort.sort(def, pending2);
        assert order2.size() == 1 && order2.get(0).equals("c1");

        System.out.println("ALL PASSED");
    }
}
