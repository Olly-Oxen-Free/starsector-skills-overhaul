package skills_overhaul.ui.view;

import org.json.JSONObject;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.Prereq;
import skills_overhaul.ui.model.NodeStatus;

import java.util.*;

public class NodeTooltipContentTest {

    static Node node(String name, String desc, int cost, String[] clauses, String[] effects)
            throws Exception {
        List<Prereq.Clause> cl = new ArrayList<>();
        for (String c : clauses) cl.add(Prereq.parseClause(new JSONObject(c)));
        List<JSONObject> ef = new ArrayList<>();
        for (String e : effects) ef.add(new JSONObject(e));
        return new Node("n1", name, desc, 0, 2, cost, cl, ef, null);
    }

    static boolean has(NodeTooltipContent c, NodeTooltipContent.Style s, String frag) {
        for (NodeTooltipContent.Line l : c.getLines())
            if (l.style == s && l.text.contains(frag)) return true;
        return false;
    }

    static boolean hasStyle(NodeTooltipContent c, NodeTooltipContent.Style s) {
        for (NodeTooltipContent.Line l : c.getLines()) if (l.style == s) return true;
        return false;
    }

    public static void main(String[] args) throws Exception {
        Set<String> owned = new HashSet<>(Arrays.asList("a", "x"));
        Node n = node("Shield", "Tougher shields.", 2,
                new String[] {"{\"all\":[\"a\",\"b\"]}", "{\"any\":[\"x\",\"y\"]}",
                        "{\"atLeast\":2,\"of\":[\"a\",\"x\",\"z\"]}"},
                new String[] {"{\"type\":\"SO_foo\"}", "{\"type\":\"SO_bar\",\"description\":\"+10% flux\"}"});

        NodeTooltipContent c = NodeTooltipContent.build(n, NodeStatus.UNLOCKABLE, owned);
        assert has(c, NodeTooltipContent.Style.TITLE, "Shield") : "name";
        assert has(c, NodeTooltipContent.Style.COST, "2") : "cost";
        assert has(c, NodeTooltipContent.Style.DESC, "Tougher") : "desc";
        assert has(c, NodeTooltipContent.Style.EFFECT, "SO_foo") : "effect falls back to type";
        assert has(c, NodeTooltipContent.Style.EFFECT, "+10% flux") : "effect uses description";
        assert !has(c, NodeTooltipContent.Style.EFFECT, "SO_bar") : "description wins over type";
        assert has(c, NodeTooltipContent.Style.PREREQ_UNMET, "all of a, b") : "All unmet";
        assert has(c, NodeTooltipContent.Style.PREREQ_MET, "any of x, y") : "Any met";
        assert has(c, NodeTooltipContent.Style.PREREQ_MET, "at least 2 of a, x, z") : "AtLeast met";
        assert !hasStyle(c, NodeTooltipContent.Style.BLOCKED) : "unlockable has no reason";

        // PENDING still shows cost; OWNED does not
        assert hasStyle(NodeTooltipContent.build(n, NodeStatus.PENDING, owned),
                NodeTooltipContent.Style.COST) : "pending cost";
        assert !hasStyle(NodeTooltipContent.build(n, NodeStatus.OWNED, owned),
                NodeTooltipContent.Style.COST) : "owned has no cost";

        // BLOCKED reasons
        NodeTooltipContent p = NodeTooltipContent.build(n,
                NodeStatus.blocked(NodeStatus.Reason.PREREQ), owned);
        assert has(p, NodeTooltipContent.Style.BLOCKED, "prerequisites") : "prereq reason";
        NodeTooltipContent t = NodeTooltipContent.build(n,
                NodeStatus.blocked(NodeStatus.Reason.TIER), owned);
        assert has(t, NodeTooltipContent.Style.BLOCKED, "previous tier") : "tier reason";
        NodeTooltipContent b = NodeTooltipContent.build(n, NodeStatus.budget(3), owned);
        assert has(b, NodeTooltipContent.Style.BLOCKED, "short by 3 points") : "budget n";
        NodeTooltipContent b1 = NodeTooltipContent.build(n, NodeStatus.budget(1), owned);
        assert has(b1, NodeTooltipContent.Style.BLOCKED, "short by 1 point") : "budget 1";
        assert !has(b1, NodeTooltipContent.Style.BLOCKED, "1 points") : "singular";

        // Edge cases: null name -> id; null/empty desc; no clauses/effects; odd effect
        Node e = node(null, "", 1, new String[0], new String[] {"{}"});
        NodeTooltipContent ec = NodeTooltipContent.build(e, NodeStatus.UNLOCKABLE, owned);
        assert has(ec, NodeTooltipContent.Style.TITLE, "n1") : "id fallback";
        assert !hasStyle(ec, NodeTooltipContent.Style.DESC) : "empty desc";
        assert !hasStyle(ec, NodeTooltipContent.Style.PREREQ_MET)
                && !hasStyle(ec, NodeTooltipContent.Style.PREREQ_UNMET) : "no clauses";
        assert hasStyle(ec, NodeTooltipContent.Style.EFFECT) : "placeholder effect, no throw";
        Node nd = node("N", null, 1, new String[] {"{\"atLeast\":5,\"of\":[\"a\"]}"}, new String[0]);
        NodeTooltipContent nc = NodeTooltipContent.build(nd, NodeStatus.UNLOCKABLE, owned);
        assert !hasStyle(nc, NodeTooltipContent.Style.DESC) : "null desc";
        assert has(nc, NodeTooltipContent.Style.PREREQ_UNMET, "at least 5 of a") : "n > ids";

        // Adapter
        NodeTooltip tip = new NodeTooltip(c);
        assert tip.getTooltipWidth(null) != 500f : "explicit width";
        assert tip.getTooltipWidth(null) == 300f : "width 300";
        assert !tip.isTooltipExpandable(null) : "not expandable";

        System.out.println("NodeTooltipContentTest passed");
    }
}
