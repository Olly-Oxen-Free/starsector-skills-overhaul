package skills_overhaul.tree;

import org.json.JSONObject;

import java.util.*;

public class PrereqValidatorTest {
    public static void main(String[] args) throws Exception {
        // Tree:
        //   r0 at (2,0) base
        //   c1a at (1,1) requires all[r0]
        //   c1b at (3,1) requires all[r0]
        //   c2 at (2,2) requires any[c1a, c1b]
        //   c3 at (2,3) requires atLeast 2 of [c1a, c1b, c2]
        String json = "{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"r0\",\"name\":\"R0\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"c1a\",\"name\":\"C1A\",\"x\":1,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"r0\"]}]}," +
                "{\"id\":\"c1b\",\"name\":\"C1B\",\"x\":3,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"r0\"]}]}," +
                "{\"id\":\"c2\",\"name\":\"C2\",\"x\":2,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"any\":[\"c1a\",\"c1b\"]}]}," +
                "{\"id\":\"c3\",\"name\":\"C3\",\"x\":2,\"y\":3,\"cost\":1," +
                " \"prereqs\":[{\"atLeast\":2,\"of\":[\"c1a\",\"c1b\",\"c2\"]}]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));
        Map<String,Object> mem = new HashMap<>();

        assert PrereqValidator.canPurchase(def, mem, "r0");
        assert !PrereqValidator.canPurchase(def, mem, "c1a") : "c1a blocked by missing r0";
        mem.put("$skills.purchased.r0", true);
        assert PrereqValidator.canPurchase(def, mem, "c1a");
        assert PrereqValidator.canPurchase(def, mem, "c1b");
        assert !PrereqValidator.canPurchase(def, mem, "c2") : "c2 needs any(c1a,c1b)";

        mem.put("$skills.purchased.c1a", true);
        assert PrereqValidator.canPurchase(def, mem, "c2") : "c2 unlocked by c1a";
        assert !PrereqValidator.canPurchase(def, mem, "c3") : "c3 needs 2-of, only 1 owned";

        mem.put("$skills.purchased.c1b", true);
        assert PrereqValidator.canPurchase(def, mem, "c3") : "c3 unlocked: c1a + c1b = 2";

        // Pending semantics
        Map<String,Object> mem2 = new HashMap<>();
        mem2.put("$skills.purchased.r0", true);
        Set<String> pending = new HashSet<>();
        assert !PrereqValidator.canPurchaseWithPending(def, mem2, pending, "c2");
        pending.add("c1a");
        assert PrereqValidator.canPurchaseWithPending(def, mem2, pending, "c2");

        // Already-pending node rejects re-pend
        Set<String> pending2 = new HashSet<>();
        pending2.add("r0");
        assert !PrereqValidator.canPurchaseWithPending(def, new HashMap<String,Object>(), pending2, "r0");

        // Reason-returning check: OK / PREREQ / UNKNOWN on the clause tree
        Map<String,Object> mem3 = new HashMap<>();
        Set<String> none = new HashSet<>();
        assert PrereqValidator.check(def, mem3, none, "r0") == PrereqValidator.Check.OK;
        assert PrereqValidator.check(def, mem3, none, "c1a") == PrereqValidator.Check.PREREQ
                : "c1a needs r0";
        assert PrereqValidator.check(def, mem3, none, "nope") == PrereqValidator.Check.UNKNOWN;
        mem3.put("$skills.purchased.r0", true);
        assert PrereqValidator.check(def, mem3, none, "c1a") == PrereqValidator.Check.OK;
        assert PrereqValidator.check(def, mem3, none, "c2") == PrereqValidator.Check.PREREQ
                : "c2 needs any(c1a,c1b)";
        Set<String> pc1a = new HashSet<>();
        pc1a.add("c1a");
        assert PrereqValidator.check(def, mem3, pc1a, "c2") == PrereqValidator.Check.OK
                : "pending c1a satisfies any() and the tier gate";

        // Tier gate: s2 at y=2 has its prereq (s0) owned but no owned/pending y=1 node
        String sjson = "{\"tree\":\"s\",\"nodes\":[" +
                "{\"id\":\"s0\",\"name\":\"S0\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"s1\",\"name\":\"S1\",\"x\":1,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"s0\"]}]}," +
                "{\"id\":\"s2\",\"name\":\"S2\",\"x\":2,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"s0\"]}]}" +
                "]}";
        TreeDef sdef = TreeLoader.fromJSON(new JSONObject(sjson));
        Map<String,Object> smem = new HashMap<>();
        smem.put("$skills.purchased.s0", true);
        assert PrereqValidator.check(sdef, smem, none, "s2") == PrereqValidator.Check.TIER
                : "s2 skips tier 1";
        assert PrereqValidator.canPurchase(sdef, smem, "s2")
                : "canPurchase stays prereq-only";
        Set<String> ps1 = new HashSet<>();
        ps1.add("s1");
        assert PrereqValidator.check(sdef, smem, ps1, "s2") == PrereqValidator.Check.OK
                : "pending s1 opens tier 2";
        smem.put("$skills.purchased.s1", true);
        assert PrereqValidator.check(sdef, smem, none, "s2") == PrereqValidator.Check.OK
                : "owned s1 opens tier 2";

        System.out.println("ALL PASSED");
    }
}
