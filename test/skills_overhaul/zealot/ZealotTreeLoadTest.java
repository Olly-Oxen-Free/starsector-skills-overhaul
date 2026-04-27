package skills_overhaul.zealot;

import org.json.JSONObject;
import org.json.JSONTokener;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeLoader;

import java.io.File;
import java.io.FileReader;

public class ZealotTreeLoadTest {
    public static void main(String[] args) throws Exception {
        File modRoot = findModRoot();
        File f = new File(modRoot, "data/config/skills_overhaul/trees/commander_zealot.json");
        assert f.exists() : "missing: " + f;
        JSONObject j = new JSONObject(new JSONTokener(new FileReader(f)));
        TreeDef def = TreeLoader.fromJSON(j);

        assert def.getTree().equals("commander_zealot");
        assert def.getNodes().size() == 21 : "expected 21 nodes, got " + def.getNodes().size();
        assert def.getT0() != null && def.getT0().getId().equals("zealot_t0");
        assert def.getT5() != null && def.getT5().getId().equals("zealot_t5");

        assert def.getAt(2, 0).getId().equals("zealot_t0");
        assert def.getAt(2, 5).getId().equals("zealot_t5");
        assert def.getAt(0, 1).getId().equals("zealot_t1_hammerfall");
        assert def.getAt(4, 4) == null : "T4 column 4 empty";

        for (Node n : def.getNodes()) {
            for (String ref : n.getReferencedPrereqIds()) {
                assert def.getNode(ref) != null
                        : "node " + n.getId() + " references unknown " + ref;
            }
        }

        System.out.println("ALL PASSED");
    }

    private static File findModRoot() {
        File d = new File(".").getAbsoluteFile();
        for (int i = 0; i < 8 && d != null; i++) {
            if (new File(d, "mod_info.json").exists()) return d;
            d = d.getParentFile();
        }
        throw new IllegalStateException("could not locate mod root");
    }
}
