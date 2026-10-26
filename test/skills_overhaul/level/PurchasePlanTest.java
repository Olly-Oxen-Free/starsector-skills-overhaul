package skills_overhaul.level;

import org.json.JSONObject;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeLoader;

import java.util.*;

public class PurchasePlanTest {
    public static void main(String[] args) throws Exception {
        // Tab with two defs:
        //   ta: a0 (2,0); a1 (1,1) all[a0]; a2 (2,2) all[a1]; aSkip (1,2) all[a0]
        //   tb: b0 (2,0); b1 (1,1) all[b0]
        String ja = "{\"tree\":\"ta\",\"nodes\":[" +
                "{\"id\":\"a0\",\"name\":\"A0\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"a1\",\"name\":\"A1\",\"x\":1,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"a0\"]}]}," +
                "{\"id\":\"a2\",\"name\":\"A2\",\"x\":2,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"a1\"]}]}," +
                "{\"id\":\"aSkip\",\"name\":\"ASkip\",\"x\":1,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"a0\"]}]}" +
                "]}";
        String jb = "{\"tree\":\"tb\",\"nodes\":[" +
                "{\"id\":\"b0\",\"name\":\"B0\",\"x\":2,\"y\":0,\"cost\":2,\"prereqs\":[]}," +
                "{\"id\":\"b1\",\"name\":\"B1\",\"x\":1,\"y\":1,\"cost\":2," +
                " \"prereqs\":[{\"all\":[\"b0\"]}]}" +
                "]}";
        TreeDef da = TreeLoader.fromJSON(new JSONObject(ja));
        TreeDef db = TreeLoader.fromJSON(new JSONObject(jb));
        List<TreeDef> defs = Arrays.asList(da, db);

        // All-or-nothing: third node in topo order is unaffordable
        Map<String,Object> mem = new HashMap<>();
        SkillMemory.addPoints(mem, Tree.PILOT, 2);
        Map<String,Object> before = new HashMap<>(mem);
        PurchasePlan.Result r = PurchasePlan.dryRun(mem, Tree.PILOT, defs,
                Arrays.asList("a2", "a1", "a0"));
        assert !r.success : "batch of 3 with 2 points fails";
        assert "a2".equals(r.failedNodeId) : "failure names the third node, got " + r.failedNodeId;
        assert r.reason == PurchasePlan.Reason.BUDGET : "reason " + r.reason;
        assert r.shortBy == 1 : "short 1, got " + r.shortBy;
        assert mem.equals(before) : "input snapshot unchanged after failed batch";

        // Affordable batch spanning two defs: topo order per def, concatenated in def order
        Map<String,Object> rich = new HashMap<>();
        SkillMemory.addPoints(rich, Tree.PILOT, 10);
        Map<String,Object> richBefore = new HashMap<>(rich);
        r = PurchasePlan.dryRun(rich, Tree.PILOT, defs, Arrays.asList("b1", "a2", "b0", "a1", "a0"));
        assert r.success : "affordable batch succeeds: " + r.reason + " " + r.failedNodeId;
        assert r.order.equals(Arrays.asList("a0", "a1", "a2", "b0", "b1")) : "order " + r.order;
        assert rich.equals(richBefore) : "input snapshot unchanged after successful dry run";
        assert SkillMemory.getPoints(r.after, Tree.PILOT) == 3 : "dry run spent 7 on the copy";
        assert Boolean.TRUE.equals(r.after.get(SkillMemory.purchasedKey("b1")));

        // Empty batch: success, no-op
        r = PurchasePlan.dryRun(rich, Tree.PILOT, defs, Collections.<String>emptyList());
        assert r.success && r.order.isEmpty() : "empty batch is a no-op success";

        // Unknown id and already-owned node are rejected
        r = PurchasePlan.dryRun(rich, Tree.PILOT, defs, Arrays.asList("a0", "ghost"));
        assert !r.success && r.reason == PurchasePlan.Reason.UNKNOWN && "ghost".equals(r.failedNodeId);
        Map<String,Object> owned = new HashMap<>(rich);
        owned.put(SkillMemory.purchasedKey("a0"), true);
        r = PurchasePlan.dryRun(owned, Tree.PILOT, defs, Arrays.asList("a0"));
        assert !r.success && r.reason == PurchasePlan.Reason.OWNED : "owned node rejected";

        // Prereq and tier failures name the node
        r = PurchasePlan.dryRun(rich, Tree.PILOT, defs, Arrays.asList("a1"));
        assert !r.success && r.reason == PurchasePlan.Reason.PREREQ && "a1".equals(r.failedNodeId);
        r = PurchasePlan.dryRun(owned, Tree.PILOT, defs, Arrays.asList("aSkip"));
        assert !r.success && r.reason == PurchasePlan.Reason.TIER && "aSkip".equals(r.failedNodeId);
        // A tier-1 batch node supports aSkip even with no prereq edge between them
        r = PurchasePlan.dryRun(owned, Tree.PILOT, defs, Arrays.asList("aSkip", "a1"));
        assert r.success : "tier support from the batch: " + r.reason;

        // Pool locked to another tree is not usable
        Map<String,Object> locked = new HashMap<>();
        locked.put(SpendService.POOL_UNALLOC, 5f);
        locked.put(SpendService.POOL_LOCK_KEY, "COMMANDER");
        Map<String,Object> lockedBefore = new HashMap<>(locked);
        r = PurchasePlan.dryRun(locked, Tree.PILOT, defs, Arrays.asList("a0"));
        assert !r.success && r.reason == PurchasePlan.Reason.BUDGET && r.shortBy == 1
                : "pool locked to COMMANDER";
        assert locked.equals(lockedBefore);

        System.out.println("ALL PASSED");
    }
}
