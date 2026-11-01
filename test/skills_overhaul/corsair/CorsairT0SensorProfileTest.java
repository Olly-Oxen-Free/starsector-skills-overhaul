package skills_overhaul.corsair;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regression for review 2026-11-01-2306-effect B4: corsair_t0's -10% sensor profile must
 * be applied once. The tree data carries it as a corsair_stat_bonus effect, so
 * CorsairT0Handler must not apply its own detected-range multiplier as well
 * (0.9 x 0.9 = -19%). The handler needs the live fleet, so it is checked on source.
 */
public class CorsairT0SensorProfileTest {
    public static void main(String[] args) throws Exception {
        String data = read("data/config/skills_overhaul/trees/commander_corsair.json");
        Matcher node = Pattern.compile("\"id\":\\s*\"corsair_t0\".*?\\]\\s*\\}", Pattern.DOTALL).matcher(data);
        assert node.find() : "corsair_t0 node not found";
        String t0 = node.group();
        int fromData = count(t0, "\"sensorProfileMod\"");
        assert fromData == 1 : "expected one sensorProfileMod on corsair_t0 in data, got " + fromData;

        String handler = read("src/skills_overhaul/effect/handlers/corsair/CorsairT0Handler.java");
        assert !Pattern.compile("getDetectedRangeMod\\(\\)\\s*\\.modifyMult").matcher(handler).find()
                : "CorsairT0Handler applies sensor profile on top of the data effect";

        System.out.println("ALL PASSED");
    }

    static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }

    /** Tests run from jars/classes (see build.sh); walk up to the mod root. */
    static String read(String rel) throws Exception {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            Path p = dir.resolve(rel);
            if (Files.exists(p)) return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot find " + rel);
    }
}
