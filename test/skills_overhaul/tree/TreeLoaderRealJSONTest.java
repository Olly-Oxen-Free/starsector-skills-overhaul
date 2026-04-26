package skills_overhaul.tree;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;

public class TreeLoaderRealJSONTest {
    public static void main(String[] args) throws Exception {
        File modRoot = findModRoot();
        for (String tab : new String[]{"pilot", "commander", "admin"}) {
            File indexFile = new File(modRoot, "data/config/skills_overhaul/trees/" + tab + "_index.json");
            JSONObject index = new JSONObject(new JSONTokener(new FileReader(indexFile)));
            String[] keys = jsonStringArray(index.getJSONArray("trees"));
            assert keys.length > 0 : tab + " has no trees in index";

            for (String key : keys) {
                File path = new File(modRoot, "data/config/skills_overhaul/trees/" + tab + "_" + key + ".json");
                assert path.exists() : "missing tree file: " + path;
                JSONObject j = new JSONObject(new JSONTokener(new FileReader(path)));
                TreeDef def = TreeLoader.fromJSON(j);
                assert def.getT0() != null : tab + "_" + key + " missing T0";
                assert def.getNodes().size() > 0 : tab + "_" + key + " has no nodes";
                for (Node n : def.getNodes()) {
                    for (String ref : n.getReferencedPrereqIds()) {
                        assert def.getNode(ref) != null
                                : tab + "_" + key + ": " + n.getId() + " refs missing " + ref;
                    }
                }
            }
        }
        System.out.println("ALL PASSED");
    }

    private static String[] jsonStringArray(org.json.JSONArray a) throws Exception {
        String[] out = new String[a.length()];
        for (int i = 0; i < a.length(); i++) out[i] = a.getString(i);
        return out;
    }

    /** Walk up from cwd looking for mod_info.json — handles tests run from jars/classes. */
    private static File findModRoot() {
        File d = new File(".").getAbsoluteFile();
        for (int i = 0; i < 8 && d != null; i++) {
            if (new File(d, "mod_info.json").exists()) return d;
            d = d.getParentFile();
        }
        throw new IllegalStateException("could not locate mod root (mod_info.json) from "
                + new File(".").getAbsolutePath());
    }
}
