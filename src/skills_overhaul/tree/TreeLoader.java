package skills_overhaul.tree;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public final class TreeLoader {

    public static TreeDef loadFromSettings(String tree) throws Exception {
        JSONObject j = Global.getSettings().getMergedJSONForMod(
                "data/config/skills_overhaul/" + tree + "_tree.json", "skills_overhaul");
        return fromJSON(j);
    }

    public static TreeDef fromJSON(JSONObject o) throws JSONException {
        String tree = o.getString("tree");
        List<String> lanes = new ArrayList<>();
        JSONArray la = o.getJSONArray("lanes");
        for (int i = 0; i < la.length(); i++) lanes.add(la.getString(i));

        Map<String, Node> byId = new LinkedHashMap<>();
        JSONArray na = o.getJSONArray("nodes");
        for (int i = 0; i < na.length(); i++) {
            Node n = Node.fromJSON(na.getJSONObject(i));
            byId.put(n.getId(), n);
        }

        JSONArray ma = o.optJSONArray("merges");
        if (ma != null) {
            for (int i = 0; i < ma.length(); i++) {
                Node m = mergeFromJSON(ma.getJSONObject(i));
                byId.put(m.getId(), m);
            }
        }

        return new TreeDef(tree, lanes, byId);
    }

    /**
     * Parses a merge-node entry. Enforces parentLanes.length == 2 invariant.
     * Derives lane = "merge:<a>_<b>" with parent lanes sorted alphabetically
     * so lane naming is deterministic regardless of JSON ordering.
     */
    private static Node mergeFromJSON(JSONObject o) throws JSONException {
        JSONArray pla = o.getJSONArray("parentLanes");
        if (pla.length() != 2) {
            throw new JSONException("merge node " + o.optString("id", "?")
                    + ": parentLanes must have length 2, got " + pla.length());
        }
        List<String> parents = new ArrayList<>();
        parents.add(pla.getString(0));
        parents.add(pla.getString(1));
        List<String> sorted = new ArrayList<>(parents);
        Collections.sort(sorted);
        String derivedLane = "merge:" + sorted.get(0) + "_" + sorted.get(1);

        List<String> pre = new ArrayList<>();
        JSONArray pa = o.optJSONArray("prereqs");
        if (pa != null) for (int i = 0; i < pa.length(); i++) pre.add(pa.getString(i));

        List<JSONObject> fx = new ArrayList<>();
        JSONArray fa = o.optJSONArray("effects");
        if (fa != null) for (int i = 0; i < fa.length(); i++) fx.add(fa.getJSONObject(i));

        return new Node(
                o.getString("id"),
                o.getString("name"),
                o.optString("desc", ""),
                derivedLane,
                o.optInt("tier", 0),
                o.optInt("slot", 1),
                o.optInt("cost", 1),
                Collections.unmodifiableList(pre),
                Collections.unmodifiableList(parents),
                o.optBoolean("capstone", false),
                true, // isMerge
                Collections.unmodifiableList(fx),
                o.optString("icon", null));
    }
}
