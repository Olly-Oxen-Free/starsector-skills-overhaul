package skills_overhaul.tree;

import java.util.*;

public final class TreeDef {
    private final String tree;
    private final List<String> lanes;
    private final Map<String, Node> byId;

    public TreeDef(String tree, List<String> lanes, Map<String, Node> byId) {
        this.tree = tree;
        this.lanes = Collections.unmodifiableList(lanes);
        this.byId = Collections.unmodifiableMap(byId);
    }

    public String getTree() { return tree; }
    public List<String> getLanes() { return lanes; }
    public Collection<Node> getNodes() { return byId.values(); }
    public Node getNode(String id) { return byId.get(id); }

    public List<Node> getRootNodes() {
        List<Node> out = new ArrayList<>();
        for (Node n : byId.values()) if (n.getPrereqs().isEmpty()) out.add(n);
        return out;
    }

    public List<Node> getMergeNodes() {
        List<Node> out = new ArrayList<>();
        for (Node n : byId.values()) {
            if (n.getParentLanes() != null && n.getParentLanes().size() == 2) out.add(n);
        }
        return out;
    }
}
