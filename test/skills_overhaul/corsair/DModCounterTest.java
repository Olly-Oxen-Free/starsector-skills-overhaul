package skills_overhaul.corsair;

import skills_overhaul.effect.handlers.corsair.DModCounterAccess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regression for review 2026-11-01-2306-effect B5: d-mods are recognised by the hull mod
 * spec's "dmod" tag, not by id pattern. Vanilla d-mod ids such as degraded_engines or
 * faulty_grid match none of the old patterns, so Scrapheap and Scrappy counted 0.
 */
public class DModCounterTest {
    public static void main(String[] args) throws Exception {
        // Ids tagged "dmod" in the base game's hull_mods.csv
        final Set<String> tagged = new HashSet<>(Arrays.asList(
                "degraded_engines", "faulty_grid", "compromised_structure", "glitched_sensors"));
        List<String> variant = Arrays.asList(
                "degraded_engines", "faulty_grid", "compromised_structure", "glitched_sensors",
                "heavyarmor", "hardenedshieldemitter");

        assert DModCounterAccess.count(variant, tagged) == 4 : "vanilla d-mods must be counted by tag";
        assert DModCounterAccess.count(Arrays.asList("heavyarmor"), tagged) == 0 : "regular hull mods are not d-mods";
        assert DModCounterAccess.count(null, tagged) == 0 : "null hull mod list";

        // Both corsair call sites must use the tag-based counter, not id heuristics.
        for (String rel : new String[]{
                "src/skills_overhaul/effect/handlers/corsair/DModDpScalingHandler.java",
                "src/skills_overhaul/effect/handlers/corsair/CorsairCombatPlugin.java"}) {
            String code = CorsairT0SensorProfileTest.read(rel);
            assert code.contains("DModCounter.count(") : rel + " does not use DModCounter";
            assert !code.contains("\"_dmod\"") && !code.contains("\"_d_\"")
                    : rel + " still counts d-mods by id pattern";
        }

        System.out.println("ALL PASSED");
    }
}
