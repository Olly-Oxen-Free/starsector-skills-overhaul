package skills_overhaul.tree;

import org.json.JSONObject;

public class NodeSlotTest {
    public static void main(String[] args) throws Exception {
        // Default slot = 1 (center) when omitted
        Node defaultNode = Node.fromJSON(new JSONObject(
                "{\"id\":\"n1\",\"name\":\"n1\",\"lane\":\"gunner\",\"tier\":1,\"cost\":1,\"prereqs\":[]}"));
        assert defaultNode.getSlot() == 1 : "default slot should be 1, got " + defaultNode.getSlot();

        // Explicit slot = 0 (left)
        Node leftNode = Node.fromJSON(new JSONObject(
                "{\"id\":\"n2\",\"name\":\"n2\",\"lane\":\"gunner\",\"tier\":1,\"slot\":0,\"cost\":1,\"prereqs\":[]}"));
        assert leftNode.getSlot() == 0 : "slot=0, got " + leftNode.getSlot();

        // Explicit slot = 2 (right)
        Node rightNode = Node.fromJSON(new JSONObject(
                "{\"id\":\"n3\",\"name\":\"n3\",\"lane\":\"gunner\",\"tier\":1,\"slot\":2,\"cost\":1,\"prereqs\":[]}"));
        assert rightNode.getSlot() == 2 : "slot=2, got " + rightNode.getSlot();

        System.out.println("ALL PASSED");
    }
}
