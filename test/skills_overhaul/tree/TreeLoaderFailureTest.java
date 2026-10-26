package skills_overhaul.tree;

import org.json.JSONArray;
import org.json.JSONObject;
import skills_overhaul.state.Tree;

import java.lang.reflect.Field;
import java.util.*;

public class TreeLoaderFailureTest {
    private static String tree(String name, String nodeId) {
        return "{\"tree\":\"" + name + "\",\"nodes\":["
                + "{\"id\":\"" + nodeId + "\",\"name\":\"N\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}]}";
    }

    public static void main(String[] args) throws Exception {
        final Map<String, String> files = new HashMap<>();
        files.put("good", tree("commander_good", "good_root"));
        files.put("broken", "{\"tree\":\"commander_broken\",\"nodes\":[{\"id\":\"x\"");  // malformed
        files.put("other", tree("commander_other", "other_root"));
        files.put("dup", tree("commander_dup", "good_root"));   // reuses good_root

        TreeLoader.TreeSource src = new TreeLoader.TreeSource() {
            public JSONObject get(String key) throws Exception { return new JSONObject(files.get(key)); }
        };

        // Malformed tree: skipped, others load, key reported.
        List<String> failed = new ArrayList<>();
        List<TreeDef> loaded = TreeLoader.loadTrees("commander",
                new JSONArray("[\"good\",\"broken\",\"other\",\"dup\"]"), src, failed);
        assert loaded.size() == 3 : loaded.size();
        assert failed.equals(Collections.singletonList("broken")) : failed;

        // Duplicate id across trees: later tree marked failed, registry does not throw.
        Map<Tree, List<TreeDef>> in = new EnumMap<>(Tree.class);
        Map<Tree, List<String>> bad = new EnumMap<>(Tree.class);
        for (Tree t : Tree.values()) { in.put(t, new ArrayList<TreeDef>()); bad.put(t, new ArrayList<String>()); }
        in.put(Tree.COMMANDER, loaded);
        bad.put(Tree.COMMANDER, failed);
        TreeRegistry.install(in, bad);

        List<TreeDef> kept = TreeRegistry.getTrees(Tree.COMMANDER);
        assert kept.size() == 2 : kept.size();
        assert kept.get(0).getTree().equals("commander_good");
        assert kept.get(1).getTree().equals("commander_other");
        List<String> f = TreeRegistry.getFailed(Tree.COMMANDER);
        assert f.contains("broken") && f.contains("dup") && f.size() == 2 : f;
        assert TreeRegistry.getFailed(Tree.PILOT).isEmpty();

        Field fd = TreeRegistry.class.getDeclaredField("defs");
        fd.setAccessible(true);
        ((Map<?, ?>) fd.get(null)).clear();
        System.out.println("ALL PASSED");
    }
}
