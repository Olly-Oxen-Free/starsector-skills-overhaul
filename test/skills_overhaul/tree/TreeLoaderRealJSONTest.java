package skills_overhaul.tree;

import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;

public class TreeLoaderRealJSONTest {

    public static void main(String[] args) throws Exception {
        File dataDir = findDataDir();
        if (dataDir == null) throw new RuntimeException("couldn't locate data/config/skills_overhaul/");

        verify(dataDir, "pilot",     new String[]{"gunner", "ace", "defender"});
        verify(dataDir, "commander", new String[]{"tactics", "logistics", "doctrine"});
        verify(dataDir, "admin",     new String[]{"industry", "trade", "stability"});

        System.out.println("ALL PASSED");
    }

    private static void verify(File dataDir, String tree, String[] expectedLanes) throws Exception {
        File f = new File(dataDir, tree + "_tree.json");
        String raw = new String(Files.readAllBytes(f.toPath()));
        TreeDef def = TreeLoader.fromJSON(new JSONObject(raw));

        assert def != null : tree + ": null TreeDef";
        assert def.getTree().equals(tree) : tree + ": tree name mismatch";
        assert def.getNodes().size() >= 14 : tree + ": expected >=14 nodes, got " + def.getNodes().size();

        for (String lane : expectedLanes) {
            assert def.getLanes().contains(lane) : tree + ": missing lane " + lane;
        }

        int capstones = 0;
        for (Node n : def.getNodes()) if (n.isCapstone()) capstones++;
        assert capstones >= 3 : tree + ": expected >=3 capstones";
        assert !def.getRootNodes().isEmpty() : tree + ": no root nodes";

        // Merge nodes parse correctly (zero allowed; when present, invariants hold)
        for (Node m : def.getMergeNodes()) {
            assert m.isMerge() : tree + ": " + m.getId() + " not marked merge";
            assert m.getParentLanes().size() == 2 : tree + ": " + m.getId() + " bad parentLanes";
            assert m.getLane().startsWith("merge:") : tree + ": " + m.getId() + " lane should start 'merge:'";
        }

        // Slot invariant: every node has slot in [0,2]
        for (Node n : def.getNodes()) {
            int s = n.getSlot();
            assert s >= 0 && s <= 2 : tree + ": " + n.getId() + " slot=" + s + " out of [0,2]";
        }

        System.out.println("  " + tree + ": " + def.getNodes().size() + " nodes, "
                + def.getMergeNodes().size() + " merges, " + capstones + " capstones");
    }

    private static File findDataDir() {
        File cur = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && cur != null; i++) {
            File candidate = new File(cur, "data/config/skills_overhaul");
            if (candidate.isDirectory()) return candidate;
            cur = cur.getParentFile();
        }
        return null;
    }
}
