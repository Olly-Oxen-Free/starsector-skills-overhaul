package skills_overhaul.tree;

import org.json.JSONObject;

import java.util.*;

/**
 * Review 2026-11-02-0039-ui B1: the previous-tier gate must hold at commit,
 * not only when a node is pended.
 */
public class PendingTierGateTest {
    public static void main(String[] args) throws Exception {
        // Shape of pilot_defense: tier-2 node whose prereq is the tier-0 root.
        //   r0 (y0); a1 (y1, all[r0]); b2 (y2, all[r0]); c3 (y3, all[b2])
        String json = "{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"r0\",\"name\":\"R0\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"a1\",\"name\":\"A1\",\"x\":0,\"y\":1,\"cost\":1,\"prereqs\":[{\"all\":[\"r0\"]}]}," +
                "{\"id\":\"b2\",\"name\":\"B2\",\"x\":1,\"y\":2,\"cost\":1,\"prereqs\":[{\"all\":[\"r0\"]}]}," +
                "{\"id\":\"c3\",\"name\":\"C3\",\"x\":1,\"y\":3,\"cost\":1,\"prereqs\":[{\"all\":[\"b2\"]}]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));
        Map<String,Object> mem = new HashMap<>();
        mem.put("$skills.purchased.r0", true);
        Set<String> none = Collections.<String>emptySet();

        // Gate itself.
        assert PrereqValidator.tierGateSatisfied(def, mem, none, "r0") : "tier 0 always open";
        assert PrereqValidator.tierGateSatisfied(def, mem, none, "a1");
        assert !PrereqValidator.tierGateSatisfied(def, mem, none, "b2") : "no tier-1 node owned";
        assert PrereqValidator.tierGateSatisfied(def, mem, Collections.singleton("a1"), "b2") : "pending tier-1 counts";

        // Un-pending a1 must take b2 (gate) and c3 (prereq on b2) with it.
        Set<String> pending = new LinkedHashSet<>(Arrays.asList("a1", "b2", "c3"));
        assert PrereqValidator.prunePending(def, mem, pending).isEmpty() : "valid set is untouched";
        pending.remove("a1");
        Set<String> removed = PrereqValidator.prunePending(def, mem, pending);
        assert pending.isEmpty() : "b2 and c3 dropped, left " + pending;
        assert removed.equals(new HashSet<>(Arrays.asList("b2", "c3")));

        // With a1 owned, b2 and c3 stay.
        mem.put("$skills.purchased.a1", true);
        Set<String> p2 = new LinkedHashSet<>(Arrays.asList("b2", "c3"));
        assert PrereqValidator.prunePending(def, mem, p2).isEmpty() && p2.size() == 2;

        // Commit order must buy lower tiers first: b2 has no prereq edge to a1,
        // so insertion order alone would buy b2 before the tier-1 node it needs.
        Set<String> p3 = new LinkedHashSet<>(Arrays.asList("b2", "a1"));
        List<String> order = PendingPurchaseTopoSort.sort(def, p3);
        assert order.equals(Arrays.asList("a1", "b2")) : "tier order, got " + order;

        System.out.println("ALL PASSED");
    }
}
