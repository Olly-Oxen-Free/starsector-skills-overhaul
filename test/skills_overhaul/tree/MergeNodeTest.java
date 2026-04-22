package skills_overhaul.tree;

import org.json.JSONObject;

import java.util.List;

public class MergeNodeTest {
    public static void main(String[] args) throws Exception {
        // Merge entry parses into a Node with derived lane = "merge:<a>_<b>" alphabetical.
        String json = "{" +
                "\"tree\":\"pilot\"," +
                "\"lanes\":[\"gunner\",\"ace\",\"defender\"]," +
                "\"nodes\":[" +
                "  {\"id\":\"g2\",\"name\":\"g2\",\"lane\":\"gunner\",\"tier\":2,\"cost\":1,\"prereqs\":[]}," +
                "  {\"id\":\"a2\",\"name\":\"a2\",\"lane\":\"ace\",\"tier\":2,\"cost\":1,\"prereqs\":[]}" +
                "]," +
                "\"merges\":[" +
                "  {\"id\":\"m3\",\"name\":\"Elite\",\"tier\":3,\"slot\":1,\"cost\":2," +
                "   \"parentLanes\":[\"gunner\",\"ace\"]," +
                "   \"prereqs\":[\"g2\",\"a2\"]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));

        Node merge = def.getNode("m3");
        assert merge != null : "merge node not loaded";
        assert merge.isMerge() : "expected isMerge=true";
        // alphabetical: ace < gunner, so lane = "merge:ace_gunner"
        assert "merge:ace_gunner".equals(merge.getLane())
                : "expected merge:ace_gunner, got " + merge.getLane();
        assert merge.getParentLanes().size() == 2 : "parentLanes.length should be 2";
        assert merge.getTier() == 3 : "tier";
        assert merge.getSlot() == 1 : "slot";
        assert merge.getCost() == 2 : "cost";
        assert merge.getPrereqs().contains("g2") && merge.getPrereqs().contains("a2");

        // getMergeNodes helper
        List<Node> mergeOnly = def.getMergeNodes();
        assert mergeOnly.size() == 1 : "expected 1 merge node";
        assert mergeOnly.get(0).getId().equals("m3");

        // Invariant enforced: parentLanes.length != 2 throws
        String badJson = "{" +
                "\"tree\":\"pilot\",\"lanes\":[\"gunner\"]," +
                "\"nodes\":[]," +
                "\"merges\":[" +
                "  {\"id\":\"bad\",\"name\":\"bad\",\"tier\":3,\"cost\":1," +
                "   \"parentLanes\":[\"gunner\"],\"prereqs\":[]}]}";
        boolean threw = false;
        try { TreeLoader.fromJSON(new JSONObject(badJson)); }
        catch (Exception e) { threw = true; }
        assert threw : "expected exception on parentLanes.length != 2";

        System.out.println("ALL PASSED");
    }
}
