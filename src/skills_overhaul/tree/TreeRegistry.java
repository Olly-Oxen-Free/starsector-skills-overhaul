package skills_overhaul.tree;

import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

public final class TreeRegistry {
    private static final Map<Tree, TreeDef> defs = new HashMap<>();

    public static void load() throws Exception {
        defs.put(Tree.PILOT,     TreeLoader.loadFromSettings("pilot"));
        defs.put(Tree.COMMANDER, TreeLoader.loadFromSettings("commander"));
        defs.put(Tree.ADMIN,     TreeLoader.loadFromSettings("admin"));
    }

    public static TreeDef get(Tree t) {
        TreeDef d = defs.get(t);
        if (d == null) throw new IllegalStateException("TreeRegistry not loaded for " + t);
        return d;
    }
}
