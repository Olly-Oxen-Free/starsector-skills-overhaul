package skills_overhaul.tree;

import org.json.JSONObject;

import java.util.*;

public class PrereqValidatorTest {
    public static void main(String[] args) throws Exception {
        String json = "{" +
                "\"tree\":\"pilot\"," +
                "\"lanes\":[\"gunner\",\"ace\",\"defender\"]," +
                "\"nodes\":[" +
                "  {\"id\":\"r1\",\"name\":\"r1\",\"lane\":\"gunner\",\"tier\":1,\"cost\":1,\"prereqs\":[]}," +
                "  {\"id\":\"g1\",\"name\":\"g1\",\"lane\":\"gunner\",\"tier\":2,\"cost\":1,\"prereqs\":[\"r1\"]}," +
                "  {\"id\":\"g2\",\"name\":\"g2\",\"lane\":\"gunner\",\"tier\":3,\"cost\":1,\"prereqs\":[\"g1\"]}," +
                "  {\"id\":\"a1\",\"name\":\"a1\",\"lane\":\"ace\",\"tier\":2,\"cost\":1,\"prereqs\":[\"r1\"]}" +
                "]," +
                "\"merges\":[" +
                "  {\"id\":\"m1\",\"name\":\"m1\",\"tier\":4,\"slot\":1,\"cost\":2," +
                "   \"parentLanes\":[\"gunner\",\"ace\"]," +
                "   \"prereqs\":[\"g1\",\"a1\"]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));
        Map<String,Object> mem = new HashMap<>();

        // --- canPurchase (owned only) ---
        assert PrereqValidator.canPurchase(def, mem, "r1") : "r1 purchasable";
        assert !PrereqValidator.canPurchase(def, mem, "g1") : "g1 blocked by missing r1";
        mem.put("$skills.purchased.r1", true);
        assert PrereqValidator.canPurchase(def, mem, "g1") : "g1 unlocked after r1";
        assert !PrereqValidator.canPurchase(def, mem, "m1") : "m1 blocked by missing g1+a1";
        mem.put("$skills.purchased.g1", true);
        mem.put("$skills.purchased.a1", true);
        assert PrereqValidator.canPurchase(def, mem, "m1") : "m1 unlocked after g1+a1";

        // --- canPurchaseWithPending (owned ∪ pending satisfies prereqs) ---
        Map<String,Object> mem2 = new HashMap<>();
        mem2.put("$skills.purchased.r1", true);
        Set<String> pending = new HashSet<>();
        // g2 needs g1; g1 not owned yet, but if pending...
        assert !PrereqValidator.canPurchaseWithPending(def, mem2, pending, "g2")
                : "g2 blocked when g1 neither owned nor pending";
        pending.add("g1");
        assert PrereqValidator.canPurchaseWithPending(def, mem2, pending, "g2")
                : "g2 unlocks when g1 pending";

        // merge node with both parents pending
        Map<String,Object> mem3 = new HashMap<>();
        mem3.put("$skills.purchased.r1", true);
        Set<String> pending3 = new HashSet<>();
        pending3.add("g1");
        pending3.add("a1");
        assert PrereqValidator.canPurchaseWithPending(def, mem3, pending3, "m1")
                : "m1 unlocks with both parent lanes' prereq nodes pending";

        // already-pending node reports false (can't re-pend)
        Set<String> pending4 = new HashSet<>();
        pending4.add("r1");
        assert !PrereqValidator.canPurchaseWithPending(def, new HashMap<String,Object>(), pending4, "r1")
                : "already-pending node cannot be pended again";

        System.out.println("ALL PASSED");
    }
}
