package skills_overhaul.tree;

import org.json.JSONObject;

public class TreeLoaderTest {
    public static void main(String[] args) throws Exception {
        String json = "{\"tree\":\"x\",\"nodes\":[" +
                "{\"id\":\"r\",\"name\":\"R\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":1,\"y\":1,\"cost\":2," +
                " \"prereqs\":[{\"all\":[\"r\"]}]," +
                " \"effects\":[{\"channel\":\"base\",\"type\":\"foo\"}]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(json));

        assert def.getTree().equals("x");
        assert def.getNodes().size() == 2;
        assert def.getT0().getId().equals("r");
        assert def.getAt(1, 1).getId().equals("a");
        assert def.getAt(2, 0).getId().equals("r");
        assert def.getAt(0, 0) == null : "empty cell -> null";

        Node a = def.getNode("a");
        assert a.getCost() == 2;
        assert a.getEffects().size() == 1;
        assert "base".equals(a.getEffects().get(0).optString("channel"));

        System.out.println("ALL PASSED");
    }
}
