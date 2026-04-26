package skills_overhaul.tree;

import java.util.*;

public final class TreeDef {
    private final String tree;
    private final Map<String, Node> byId;
    private final Map<Long, Node> byCoord;   // key = ((long)x << 32) | y
    private final Node t0;
    private final Node t5;

    public TreeDef(String tree, Map<String, Node> byId) {
        this.tree = tree;
        this.byId = Collections.unmodifiableMap(byId);
        Map<Long, Node> coord = new HashMap<>();
        Node base = null, cap = null;
        for (Node n : byId.values()) {
            long key = coordKey(n.getX(), n.getY());
            if (coord.containsKey(key)) {
                throw new IllegalStateException(
                        "tree " + tree + ": duplicate (x,y) coord at ("
                                + n.getX() + "," + n.getY() + ") for node " + n.getId());
            }
            coord.put(key, n);
            if (n.getY() == 0) base = n;
            if (n.getY() == 5) cap = n;
        }
        this.byCoord = Collections.unmodifiableMap(coord);
        this.t0 = base;
        this.t5 = cap;
    }

    private static long coordKey(int x, int y) { return ((long) x << 32) | (y & 0xffffffffL); }

    public String getTree()           { return tree; }
    public Collection<Node> getNodes(){ return byId.values(); }
    public Node getNode(String id)    { return byId.get(id); }
    public Node getAt(int x, int y)   { return byCoord.get(coordKey(x, y)); }
    public Node getT0()               { return t0; }
    public Node getT5()               { return t5; }

    public List<Node> getRootNodes() {
        // T0 is the only root in the grid model.
        List<Node> out = new ArrayList<>(1);
        if (t0 != null) out.add(t0);
        return out;
    }

}
