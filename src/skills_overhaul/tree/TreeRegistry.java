package skills_overhaul.tree;

import org.apache.log4j.Logger;
import skills_overhaul.state.Tree;

import java.util.*;

public final class TreeRegistry {
    private static final Map<Tree, List<TreeDef>> defs = new EnumMap<>(Tree.class);
    private static final Map<Tree, List<String>> failed = new EnumMap<>(Tree.class);

    public static void load() throws Exception {
        Map<Tree, List<TreeDef>> loaded = new EnumMap<>(Tree.class);
        Map<Tree, List<String>> bad = new EnumMap<>(Tree.class);
        for (Tree t : Tree.values()) {
            List<String> f = new ArrayList<>();
            loaded.put(t, TreeLoader.loadAllForTab(t.key, f));
            bad.put(t, f);
        }
        install(loaded, bad);
    }

    /**
     * Installs loaded trees. $skills.purchased.&lt;id&gt; is global: an id reused in another
     * tree would share ownership, and TreeLoader only checks within one file. A tree that
     * reuses an id from an earlier tree is dropped and marked failed; the earlier one wins.
     */
    static void install(Map<Tree, List<TreeDef>> loaded, Map<Tree, List<String>> failedIn) {
        defs.clear();
        failed.clear();
        Map<String, String> seen = new HashMap<>();
        for (Tree t : Tree.values()) {
            List<TreeDef> keep = new ArrayList<>();
            List<String> bad = new ArrayList<>();
            List<String> pre = failedIn.get(t);
            if (pre != null) bad.addAll(pre);
            List<TreeDef> in = loaded.get(t);
            if (in != null) for (TreeDef d : in) {
                String clash = null;
                for (Node n : d.getNodes()) {
                    String prev = seen.get(n.getId());
                    if (prev != null) { clash = "node id " + n.getId() + " already defined in " + prev; break; }
                }
                if (clash != null) {
                    Logger.getLogger(TreeRegistry.class).warn("Skills Overhaul: tree "
                            + d.getTree() + " skipped: " + clash);
                    String prefix = t.key + "_";
                    bad.add(d.getTree().startsWith(prefix) ? d.getTree().substring(prefix.length()) : d.getTree());
                    continue;
                }
                for (Node n : d.getNodes()) seen.put(n.getId(), d.getTree());
                keep.add(d);
            }
            defs.put(t, keep);
            failed.put(t, bad);
        }
    }

    /** Index keys of trees of this tab that failed to load (malformed or duplicate ids). */
    public static List<String> getFailed(Tree tab) {
        List<String> f = failed.get(tab);
        return f == null ? Collections.<String>emptyList() : Collections.unmodifiableList(f);
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
