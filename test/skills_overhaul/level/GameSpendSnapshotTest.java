package skills_overhaul.level;

import org.json.JSONObject;
import org.json.JSONTokener;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeLoader;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regression: GameSpend.snapshot() copied point pools but not
 * $skills.purchased.* flags, so PrereqValidator saw an empty owned-set and
 * every node with a prereq failed purchase in the live UI. This test verifies
 * (a) purchasedKeys() covers every node of a real tree, and (b) a snapshot-
 * shaped map that includes those flags passes canPurchase for a tier>0 node
 * whose prereqs are owned — and fails without them (the original bug).
 */
public class GameSpendSnapshotTest {
    public static void main(String[] args) throws Exception {
        File modRoot = findModRoot();
        JSONObject index = new JSONObject(new JSONTokener(new FileReader(
                new File(modRoot, "data/config/skills_overhaul/trees/pilot_index.json"))));
        String key = index.getJSONArray("trees").getString(0);
        JSONObject j = new JSONObject(new JSONTokener(new FileReader(
                new File(modRoot, "data/config/skills_overhaul/trees/pilot_" + key + ".json"))));
        TreeDef def = TreeLoader.fromJSON(j);

        // (a) purchasedKeys covers all nodes
        List<String> keys = GameSpend.purchasedKeys(def);
        assert keys.size() == def.getNodes().size() : "purchasedKeys must cover every node";
        for (Node n : def.getNodes()) {
            assert keys.contains(SkillMemory.purchasedKey(n.getId()))
                    : "missing purchased key for " + n.getId();
        }

        // Find a node with at least one prereq clause
        Node gated = null;
        for (Node n : def.getNodes()) {
            if (!n.getClauses().isEmpty() && !n.getReferencedPrereqIds().isEmpty()) {
                gated = n;
                break;
            }
        }
        assert gated != null : "tree has no prereq-gated node to test";

        // (b) without purchased flags in the map (the old snapshot), validation fails
        Map<String,Object> bare = new HashMap<String,Object>();
        assert !PrereqValidator.canPurchase(def, bare, gated.getId())
                : "gated node should fail with empty owned-set";

        // with prereq flags copied (the fixed snapshot), validation passes
        Map<String,Object> withFlags = new HashMap<String,Object>();
        for (String ref : gated.getReferencedPrereqIds()) {
            withFlags.put(SkillMemory.purchasedKey(ref), Boolean.TRUE);
        }
        assert PrereqValidator.canPurchase(def, withFlags, gated.getId())
                : "gated node " + gated.getId() + " should pass once prereqs are in the snapshot";

        System.out.println("ALL PASSED");
    }

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
