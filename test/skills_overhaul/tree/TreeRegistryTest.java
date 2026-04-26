package skills_overhaul.tree;

import org.json.JSONObject;
import skills_overhaul.state.Tree;

import java.lang.reflect.Field;
import java.util.*;

public class TreeRegistryTest {
    public static void main(String[] args) throws Exception {
        // Inject two synthetic trees into the COMMANDER slot via reflection (avoids needing Global).
        TreeDef a = TreeLoader.fromJSON(new JSONObject(
                "{\"tree\":\"commander_alpha\",\"nodes\":[" +
                "{\"id\":\"a_root\",\"name\":\"AR\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}" +
                "]}"));
        TreeDef b = TreeLoader.fromJSON(new JSONObject(
                "{\"tree\":\"commander_beta\",\"nodes\":[" +
                "{\"id\":\"b_root\",\"name\":\"BR\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}" +
                "]}"));

        Field f = TreeRegistry.class.getDeclaredField("defs");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Tree, List<TreeDef>> defs = (Map<Tree, List<TreeDef>>) f.get(null);
        defs.put(Tree.COMMANDER, Arrays.asList(a, b));

        List<TreeDef> trees = TreeRegistry.getTrees(Tree.COMMANDER);
        assert trees.size() == 2;

        assert TreeRegistry.findNode(Tree.COMMANDER, "a_root") != null;
        assert TreeRegistry.findNode(Tree.COMMANDER, "b_root") != null;
        assert TreeRegistry.findNode(Tree.COMMANDER, "missing") == null;

        TreeDef owner = TreeRegistry.findTreeOf(Tree.COMMANDER, "b_root");
        assert owner != null && owner.getTree().equals("commander_beta");

        assert TreeRegistry.totalNodeCount(Tree.COMMANDER) == 2;

        defs.clear();  // leave registry empty for downstream tests
        System.out.println("ALL PASSED");
    }
}
