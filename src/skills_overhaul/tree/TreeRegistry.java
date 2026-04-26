package skills_overhaul.tree;

import skills_overhaul.state.Tree;

import java.util.*;

public final class TreeRegistry {
    private static final Map<Tree, List<TreeDef>> defs = new EnumMap<>(Tree.class);

    public static void load() throws Exception {
        defs.put(Tree.PILOT,     TreeLoader.loadAllForTab("pilot"));
        defs.put(Tree.COMMANDER, TreeLoader.loadAllForTab("commander"));
        defs.put(Tree.ADMIN,     TreeLoader.loadAllForTab("admin"));
    }

    public static List<TreeDef> getTrees(Tree tab) {
        List<TreeDef> ts = defs.get(tab);
        if (ts == null) throw new IllegalStateException("TreeRegistry not loaded for " + tab);
        return ts;
    }

    /** Total node count across all trees of a tab. */
    public static int totalNodeCount(Tree tab) {
        int n = 0;
        for (TreeDef d : getTrees(tab)) n += d.getNodes().size();
        return n;
    }

    /** Returns the TreeDef containing the given nodeId in the given tab, or null. */
    public static TreeDef findTreeOf(Tree tab, String nodeId) {
        for (TreeDef d : getTrees(tab)) if (d.getNode(nodeId) != null) return d;
        return null;
    }

    /** Returns the Node with the given id from the given tab's trees, or null. */
    public static Node findNode(Tree tab, String nodeId) {
        TreeDef d = findTreeOf(tab, nodeId);
        return d == null ? null : d.getNode(nodeId);
    }

    /** Returns the TreeDef containing the given nodeId across all tabs, or null. */
    public static TreeDef findTreeOfAnywhere(String nodeId) {
        for (Tree t : Tree.values()) {
            TreeDef d = findTreeOf(t, nodeId);
            if (d != null) return d;
        }
        return null;
    }
}
