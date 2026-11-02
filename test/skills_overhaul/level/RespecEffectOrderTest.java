package skills_overhaul.level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Regression for review 2026-11-01-2306-effect B2.
 *
 * NodeEffectApplier.remove() only removes a T0 node's `core` channel while the node is
 * still in the active-basics set. RespecRunner must therefore remove effects before it
 * drops T0 nodes from that set. Both calls go through Global, so the order is checked
 * on the source rather than by running the respec.
 */
public class RespecEffectOrderTest {
    public static void main(String[] args) throws Exception {
        Path src = locate("src/skills_overhaul/level/RespecRunner.java");
        String code = new String(Files.readAllBytes(src), StandardCharsets.UTF_8);

        int remove = code.indexOf("NodeEffectApplier.remove(");
        int drop = code.indexOf("ActiveLaneService.onUnpurchased(");
        assert remove >= 0 : "RespecRunner no longer calls NodeEffectApplier.remove";
        assert drop >= 0 : "RespecRunner no longer calls ActiveLaneService.onUnpurchased";
        assert remove < drop
                : "effects must be removed before T0 nodes leave the active set, or `core` effects leak";

        System.out.println("ALL PASSED");
    }

    /** Tests run from jars/classes (see build.sh); walk up to the mod root. */
    static Path locate(String rel) {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            Path p = dir.resolve(rel);
            if (Files.exists(p)) return p;
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot find " + rel);
    }
}
