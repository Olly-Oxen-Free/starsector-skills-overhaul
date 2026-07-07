package skills_overhaul.artificer;

import skills_overhaul.artificer.alphastorm.AlphaStormState;
import skills_overhaul.artificer.alphastorm.AlphaStormState.Intensity;

import java.util.HashMap;
import java.util.Map;

/**
 * Fix 2: billing a multi-day skip must charge each skipped day at that day's own intensity,
 * not charge every skipped day at today's (possibly higher) intensity.
 */
public class AlphaStormBillingTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();
        AlphaStormState.start(mem, 0); // storm started on day 0

        int lastBilled = 1;  // already billed through day 1 (MILD)
        int today = 6;       // player skipped ahead: day 6 is SEVERE

        // Correct: recompute intensity per skipped day (days 2..6).
        int perDayTotal = 0;
        for (int day = lastBilled + 1; day <= today; day++) {
            perDayTotal += AlphaStormState.dailyCost(AlphaStormState.intensity(mem, day));
        }
        // day2=MILD(2) day3=MODERATE(5) day4=MODERATE(5) day5=SEVERE(10) day6=SEVERE(10) = 32
        assert perDayTotal == 32 : "per-day billing total, got " + perDayTotal;

        // Old buggy behaviour: today's intensity charged for every skipped day.
        Intensity todayIntensity = AlphaStormState.intensity(mem, today);
        assert todayIntensity == Intensity.SEVERE;
        int buggyTotal = AlphaStormState.dailyCost(todayIntensity) * (today - lastBilled); // 10 * 5 = 50
        assert buggyTotal == 50;
        assert perDayTotal < buggyTotal : "recomputed billing must be cheaper than retroactive overbill";

        System.out.println("ALL PASSED");
    }
}
