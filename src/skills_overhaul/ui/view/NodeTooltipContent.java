package skills_overhaul.ui.view;

import org.json.JSONObject;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.Prereq;
import skills_overhaul.ui.model.NodeStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pure tooltip content for one skill node: plain styled lines, no engine types.
 * {@link NodeTooltip} renders it.
 */
public final class NodeTooltipContent {

    /** Rendering style token for a line. */
    public enum Style { TITLE, COST, DESC, EFFECT, PREREQ_MET, PREREQ_UNMET, BLOCKED }

    public static final class Line {
        public final String text;
        public final Style style;
        Line(String text, Style style) { this.text = text; this.style = style; }
        @Override public String toString() { return style + ": " + text; }
    }

    private final List<Line> lines;

    private NodeTooltipContent(List<Line> lines) {
        this.lines = Collections.unmodifiableList(lines);
    }

    public List<Line> getLines() { return lines; }

    /**
     * @param ownedIds ids of owned or pending nodes, used to mark prereq clauses met or unmet
     */
    public static NodeTooltipContent build(Node node, NodeStatus status, Set<String> ownedIds) {
        List<Line> out = new ArrayList<>();
        String name = node.getName();
        out.add(new Line(name == null || name.isEmpty() ? node.getId() : name, Style.TITLE));

        if (status == null || status.state != NodeStatus.State.OWNED) {
            out.add(new Line("Cost: " + node.getCost() + (node.getCost() == 1 ? " point" : " points"),
                    Style.COST));
        }

        String desc = node.getDesc();
        if (desc != null && !desc.isEmpty()) out.add(new Line(desc, Style.DESC));

        List<JSONObject> effects = node.getEffects();
        if (effects != null) {
            for (JSONObject e : effects) {
                if (e == null) continue;
                String text = e.optString("description", "");
                if (text.isEmpty()) text = e.optString("type", "");
                if (text.isEmpty()) text = "(unnamed effect)";
                out.add(new Line(text, Style.EFFECT));
            }
        }

        List<Prereq.Clause> clauses = node.getClauses();
        if (clauses != null) {
            for (Prereq.Clause c : clauses) {
                boolean met = c.satisfied(ownedIds);
                out.add(new Line((met ? "[met] " : "[unmet] ") + describe(c),
                        met ? Style.PREREQ_MET : Style.PREREQ_UNMET));
            }
        }

        if (status != null && status.isBlocked()) {
            out.add(new Line(blockedText(status), Style.BLOCKED));
        }
        return new NodeTooltipContent(out);
    }

    static String blockedText(NodeStatus s) {
        if (s.reason == NodeStatus.Reason.TIER) {
            return "Blocked: need a node in the previous tier";
        }
        if (s.reason == NodeStatus.Reason.BUDGET) {
            return "Blocked: short by " + s.shortBy + (s.shortBy == 1 ? " point" : " points");
        }
        return "Blocked: prerequisites not met";
    }

    static String describe(Prereq.Clause c) {
        if (c instanceof Prereq.All) {
            return "all of " + join(((Prereq.All) c).getIds());
        }
        if (c instanceof Prereq.Any) {
            return "any of " + join(((Prereq.Any) c).getIds());
        }
        if (c instanceof Prereq.AtLeast) {
            Prereq.AtLeast a = (Prereq.AtLeast) c;
            return "at least " + a.getN() + " of " + join(a.getIds());
        }
        return "prerequisite";
    }

    private static String join(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(id);
        }
        return sb.toString();
    }
}
