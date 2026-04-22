package skills_overhaul.tree;

import org.json.JSONObject;

public class TreeLoaderTest {
    public static void main(String[] args) throws Exception {
        String json = "{" +
                "\"tree\":\"pilot\"," +
                "\"lanes\":[\"gunner\",\"ace\",\"defender\"]," +
                "\"nodes\":[" +
                "  {\"id\":\"so_pilot_root_1\",\"name\":\"Basic\",\"lane\":\"root\",\"tier\":0,\"cost\":1,\"prereqs\":[]}," +
                "  {\"id\":\"so_pilot_gunner_1\",\"name\":\"GD\",\"lane\":\"gunner\",\"tier\":1,\"cost\":1,\"prereqs\":[\"so_pilot_root_1\"]}," +
                "  {\"id\":\"so_pilot_gunner_cap\",\"name\":\"Cap\",\"lane\":\"gunner\",\"tier\":9,\"cost\":2,\"prereqs\":[\"so_pilot_gunner_1\"],\"capstone\":true}" +
                "]," +
                "\"merges\":[]" +
                "}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));
        assert def.getTree().equals("pilot") : "tree name";
        assert def.getNodes().size() == 3 : "3 nodes";

        Node cap = def.getNode("so_pilot_gunner_cap");
        assert cap.isCapstone() : "capstone flag";
        assert cap.getCost() == 2 : "capstone cost";
        assert cap.getPrereqs().contains("so_pilot_gunner_1") : "prereq";

        // Root nodes detected
        assert def.getRootNodes().size() == 1 : "1 root in this stub";

        System.out.println("ALL PASSED");
    }
}
