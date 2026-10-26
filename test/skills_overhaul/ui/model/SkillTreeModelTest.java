package skills_overhaul.ui.model;

import org.json.JSONObject;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeLoader;

import java.util.*;

public class SkillTreeModelTest {
    public static void main(String[] args) throws Exception {
        // Tab with two defs:
        //   ta: a0 (2,0); a1 (1,1) all[a0]; a1b (3,1) all[a0];
        //       a2 (2,2) any[a1,a1b]; a3 (2,3) atLeast 2 of [a1,a1b,a2];
        //       aSkip (1,2) all[a0]  (prereq met by a0, tier gate needs a y=1 node)
        //   tb: b0 (2,0) cost 6; b1 (1,1) all[b0]; bT (3,1) no prereqs
        String ja = "{\"tree\":\"ta\",\"nodes\":[" +
                "{\"id\":\"a0\",\"name\":\"A0\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"a1\",\"name\":\"A1\",\"x\":1,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"a0\"]}]}," +
                "{\"id\":\"a1b\",\"name\":\"A1B\",\"x\":3,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"a0\"]}]}," +
                "{\"id\":\"a2\",\"name\":\"A2\",\"x\":2,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"any\":[\"a1\",\"a1b\"]}]}," +
                "{\"id\":\"a3\",\"name\":\"A3\",\"x\":2,\"y\":3,\"cost\":1," +
                " \"prereqs\":[{\"atLeast\":2,\"of\":[\"a1\",\"a1b\",\"a2\"]}]}," +
                "{\"id\":\"aSkip\",\"name\":\"ASkip\",\"x\":1,\"y\":2,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"a0\"]}]}" +
                "]}";
        String jb = "{\"tree\":\"tb\",\"nodes\":[" +
                "{\"id\":\"b0\",\"name\":\"B0\",\"x\":2,\"y\":0,\"cost\":6,\"prereqs\":[]}," +
                "{\"id\":\"b1\",\"name\":\"B1\",\"x\":1,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"b0\"]}]}," +
                "{\"id\":\"bT\",\"name\":\"BT\",\"x\":3,\"y\":1,\"cost\":1,\"prereqs\":[]}" +
                "]}";
        TreeDef da = TreeLoader.fromJSON(new JSONObject(ja));
        TreeDef db = TreeLoader.fromJSON(new JSONObject(jb));
        List<TreeDef> defs = Arrays.asList(da, db);

        Map<String,Object> snap = new HashMap<>();
        SkillMemory.addPoints(snap, Tree.PILOT, 5);
        snap.put(SkillMemory.purchasedKey("a0"), true);
        Map<String,Object> snapBefore = new HashMap<>(snap);

        SkillTreeModel m = new SkillTreeModel(Tree.PILOT, defs, snap);

        // States and reasons across both defs
        assert m.status("a0") == NodeStatus.OWNED;
        assert m.status("a1") == NodeStatus.UNLOCKABLE;
        assert m.status("a1b") == NodeStatus.UNLOCKABLE;
        assertBlocked(m, "a2", NodeStatus.Reason.PREREQ);
        assertBlocked(m, "a3", NodeStatus.Reason.PREREQ);
        assertBlocked(m, "aSkip", NodeStatus.Reason.TIER);
        assertBlocked(m, "b1", NodeStatus.Reason.PREREQ);
        // bT: def ta owns a y=0 node, but the tier gate looks only at bT's own def
        assertBlocked(m, "bT", NodeStatus.Reason.TIER);
        assertBlocked(m, "b0", NodeStatus.Reason.BUDGET);
        assert m.status("b0").shortBy == 1 : "b0 costs 6 with 5 points: " + m.status("b0");
        assert m.status("ghost") == null : "unknown id has no status";
        assert m.statuses().size() == 9 : "every node of both defs";

        // add: unaffordable and tier-blocked nodes are rejected, pending unchanged
        SkillTreeModel.Rejection rej = m.add("b0");
        assert rej != null && "b0".equals(rej.nodeId) : "b0 rejected";
        assert rej.getReason() == NodeStatus.Reason.BUDGET && rej.status.shortBy == 1;
        assert m.getPending().isEmpty() : "pending unchanged after budget rejection";
        rej = m.add("aSkip");
        assert rej != null && rej.getReason() == NodeStatus.Reason.TIER : "aSkip tier-rejected";
        assert m.getPending().isEmpty() : "pending unchanged after tier rejection";
        rej = m.add("a2");
        assert rej != null && rej.getReason() == NodeStatus.Reason.PREREQ;
        rej = m.add("a0");
        assert rej != null && rej.status == NodeStatus.OWNED && rej.getReason() == null;
        rej = m.add("ghost");
        assert rej != null && rej.status == null : "unknown id rejected without throwing";
        assert m.getPending().isEmpty();

        // add: pending opens dependents and the tier gate, budget is cumulative
        assert m.add("a1") == null : "a1 unlockable";
        assert m.status("a1") == NodeStatus.PENDING;
        rej = m.add("a1");
        assert rej != null && rej.status == NodeStatus.PENDING : "already pending rejected";
        assert m.status("a2") == NodeStatus.UNLOCKABLE : "any(a1,a1b) via pending a1";
        assert m.status("aSkip") == NodeStatus.UNLOCKABLE : "tier 1 has pending a1";
        assert m.status("b0").shortBy == 2 : "cumulative: 1 pending + 6 vs 5: " + m.status("b0");

        assert m.add("a1b") == null;
        assert m.add("a2") == null;
        assert m.add("a3") == null;
        assert m.getPending().size() == 4;
        assert m.status("b0").shortBy == 5 : "4 pending + 6 vs 5";

        // remove: dependents re-checked with clauses, not the flat prereq list.
        // a2 keeps any(a1b); a3 keeps atLeast 2 via a1b + a2.
        List<String> removed = m.remove("a1");
        assert removed.equals(Collections.singletonList("a1")) : "only a1 removed: " + removed;
        assert m.getPending().equals(new LinkedHashSet<>(Arrays.asList("a1b", "a2", "a3")))
                : "pending " + m.getPending();
        // Now a2 loses its any() and a3 drops below atLeast 2
        removed = m.remove("a1b");
        assert new HashSet<>(removed).equals(new HashSet<>(Arrays.asList("a1b", "a2", "a3")))
                : "cascade " + removed;
        assert removed.get(0).equals("a1b");
        assert m.getPending().isEmpty();

        // remove: tier gate drops a node whose only tier-(y-1) support was removed
        assert m.add("a1") == null;
        assert m.add("aSkip") == null;
        removed = m.remove("a1");
        assert removed.equals(Arrays.asList("a1", "aSkip")) : "aSkip loses tier support: " + removed;
        assert m.getPending().isEmpty();

        assert m.remove("a0").isEmpty() : "removing a non-pending node is a no-op";
        assert snap.equals(snapBefore) : "model never mutates the snapshot";

        System.out.println("ALL PASSED");
    }

    private static void assertBlocked(SkillTreeModel m, String id, NodeStatus.Reason reason) {
        NodeStatus s = m.status(id);
        assert s != null && s.state == NodeStatus.State.BLOCKED && s.reason == reason
                : id + " expected BLOCKED(" + reason + "), got " + s;
    }
}
