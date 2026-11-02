package skills_overhaul.tree;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
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

    /**
     * Loads every tree file matching the given tab prefix from the mod's config directory.
     * Returns the parsed TreeDefs in deterministic order (sorted by tree id).
     *
     * Reads via Global.getSettings().getMergedJSONForMod for each known file. Because
     * Starsector's settings API doesn't expose a "list mod files" call, the set of files
     * to attempt is enumerated from a per-tab manifest at
     *   data/config/skills_overhaul/trees/<tabKey>_index.json
     * which is a JSON array of tree keys, e.g. ["leadership","tactics","logistics","doctrine"]
     * for the commander tab. Authors update the manifest when adding a tree.
     */
    public static List<TreeDef> loadAllForTab(String tabKey) throws Exception {
        return loadAllForTab(tabKey, new ArrayList<String>());
    }

    /** Supplies the raw JSON for one tree key; lets tests inject fixtures without Global. */
    interface TreeSource {
        JSONObject get(String key) throws Exception;
    }

    /**
     * Like {@link #loadAllForTab(String)}, but a tree that fails to load is logged at WARN,
     * skipped, and its index key appended to {@code failedOut}. The other trees still load.
     */
    public static List<TreeDef> loadAllForTab(final String tabKey, List<String> failedOut) throws Exception {
        String indexPath = "data/config/skills_overhaul/trees/" + tabKey + "_index.json";
        JSONObject index = Global.getSettings().getMergedJSONForMod(indexPath, "skills_overhaul");
        return loadTrees(tabKey, index.getJSONArray("trees"), new TreeSource() {
            public JSONObject get(String key) throws Exception {
                return Global.getSettings().getMergedJSONForMod(
                        "data/config/skills_overhaul/trees/" + tabKey + "_" + key + ".json",
                        "skills_overhaul");
            }
        }, failedOut);
    }

    static List<TreeDef> loadTrees(String tabKey, JSONArray keys, TreeSource src,
                                   List<String> failedOut) throws JSONException {
        List<TreeDef> out = new ArrayList<>(keys.length());
        for (int i = 0; i < keys.length(); i++) {
            String key = keys.getString(i);
            try {
                out.add(fromJSON(src.get(key)));
            } catch (Exception e) {
                Logger.getLogger(TreeLoader.class).warn("Skills Overhaul: failed to load tree "
                        + tabKey + "_" + key + ", skipping: " + e);
                failedOut.add(key);
            }
        }
        return out;
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
