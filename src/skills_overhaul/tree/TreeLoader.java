package skills_overhaul.tree;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public final class TreeLoader {

    public static TreeDef loadFromSettings(String tree) throws Exception {
        JSONObject j = Global.getSettings().getMergedJSONForMod(
                "data/config/skills_overhaul/trees/" + tree + ".json", "skills_overhaul");
        return fromJSON(j);
    }

    public static TreeDef fromJSON(JSONObject o) throws JSONException {
        String tree = o.getString("tree");

        Map<String, Node> byId = new LinkedHashMap<>();
        JSONArray na = o.getJSONArray("nodes");
        for (int i = 0; i < na.length(); i++) {
            Node n = Node.fromJSON(na.getJSONObject(i));
            validateCoords(tree, n);
            if (byId.containsKey(n.getId())) {
                throw new JSONException("tree " + tree + ": duplicate node id " + n.getId());
            }
            byId.put(n.getId(), n);
        }

        // Pass 2: validate T0 prereqs + cross-reference resolution.
        for (Node n : byId.values()) {
            if (n.getY() == 0 && !n.getClauses().isEmpty()) {
                throw new JSONException("tree " + tree + ": T0 node " + n.getId()
                        + " must have empty prereqs");
            }
            for (String ref : n.getReferencedPrereqIds()) {
                if (!byId.containsKey(ref)) {
                    throw new JSONException("tree " + tree + ": node " + n.getId()
                            + " references unknown prereq id " + ref);
                }
            }
        }

        // TreeDef constructor enforces unique (x,y); rethrow as JSONException for symmetry.
        try {
            return new TreeDef(tree, byId);
        } catch (IllegalStateException ise) {
            throw new JSONException(ise.getMessage());
        }
    }

    private static void validateCoords(String tree, Node n) throws JSONException {
        int x = n.getX(), y = n.getY();
        if (x < 0 || x > 4) {
            throw new JSONException("tree " + tree + ": node " + n.getId()
                    + " x=" + x + " out of range [0,4]");
        }
        if (y < 0 || y > 5) {
            throw new JSONException("tree " + tree + ": node " + n.getId()
                    + " y=" + y + " out of range [0,5]");
        }
        if ((y == 0 || y == 5) && x != 2) {
            throw new JSONException("tree " + tree + ": node " + n.getId()
                    + " at y=" + y + " must have x=2 (got " + x + ")");
        }
    }
}
