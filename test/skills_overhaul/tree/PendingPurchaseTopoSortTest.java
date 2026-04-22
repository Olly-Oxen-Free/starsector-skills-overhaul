package skills_overhaul.tree;

import org.json.JSONObject;

import java.util.*;

public class PendingPurchaseTopoSortTest {
    public static void main(String[] args) throws Exception {
        String json = "{" +
                "\"tree\":\"pilot\"," +
                "\"lanes\":[\"gunner\"]," +
                "\"nodes\":[" +
                "  {\"id\":\"r\",\"name\":\"r\",\"lane\":\"gunner\",\"tier\":1,\"cost\":1,\"prereqs\":[]}," +
                "  {\"id\":\"a\",\"name\":\"a\",\"lane\":\"gunner\",\"tier\":2,\"cost\":1,\"prereqs\":[\"r\"]}," +
                "  {\"id\":\"b\",\"name\":\"b\",\"lane\":\"gunner\",\"tier\":3,\"cost\":1,\"prereqs\":[\"a\"]}," +
                "  {\"id\":\"c\",\"name\":\"c\",\"lane\":\"gunner\",\"tier\":3,\"cost\":1,\"prereqs\":[\"a\"]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));

        // Pending {b, r, a} should sort to [r, a, b] (or any order where r before a before b)
        Set<String> pending = new LinkedHashSet<>();
        pending.add("b"); pending.add("r"); pending.add("a");
        List<String> sorted = PendingPurchaseTopoSort.sort(def, pending);
        assert sorted.size() == 3 : "size";
        assert sorted.indexOf("r") < sorted.indexOf("a") : "r before a";
        assert sorted.indexOf("a") < sorted.indexOf("b") : "a before b";

        // Owned prereqs are ignored for ordering (only in-pending edges count)
        // Here only {a, b} pending, r assumed owned — edge r->a ignored because r not in pending
        Set<String> pending2 = new LinkedHashSet<>();
        pending2.add("b"); pending2.add("a");
        List<String> sorted2 = PendingPurchaseTopoSort.sort(def, pending2);
        assert sorted2.size() == 2;
        assert sorted2.indexOf("a") < sorted2.indexOf("b") : "a before b (owned r ignored)";

        // Independent branches (b and c both depend only on a): any ordering OK, just assert
        // a comes before both.
        Set<String> pending3 = new LinkedHashSet<>();
        pending3.add("b"); pending3.add("c"); pending3.add("a");
        List<String> sorted3 = PendingPurchaseTopoSort.sort(def, pending3);
        assert sorted3.indexOf("a") < sorted3.indexOf("b");
        assert sorted3.indexOf("a") < sorted3.indexOf("c");

        System.out.println("ALL PASSED");
    }
}
