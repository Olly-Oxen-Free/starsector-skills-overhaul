package skills_overhaul.tree;

import org.json.JSONObject;

public class NodeSlotTest {
    public static void main(String[] args) throws Exception {
        // Asserts that (x,y) round-trip from JSON to Node correctly and that
        // legacy accessors (getTier/getSlot) reflect the canonical fields.
        String json = "{\"id\":\"n\",\"name\":\"N\",\"x\":3,\"y\":2,\"cost\":1,\"prereqs\":[]}";
        Node n = Node.fromJSON(new JSONObject(json));
        assert n.getX() == 3;
        assert n.getY() == 2;
        assert n.getSlot() == 3 : "legacy getSlot mirrors x";
        assert n.getTier() == 2 : "legacy getTier mirrors y";
        assert !n.isCapstone();
        assert !n.isMerge();
        assert n.getLane() == null;
        assert n.getParentLanes().isEmpty();
        System.out.println("ALL PASSED");
    }
}
