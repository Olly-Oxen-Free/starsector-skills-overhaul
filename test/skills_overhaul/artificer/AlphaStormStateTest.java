package skills_overhaul.artificer;

import skills_overhaul.artificer.alphastorm.AlphaStormState;
import skills_overhaul.artificer.alphastorm.AlphaStormState.Intensity;

import java.util.HashMap;
import java.util.Map;

public class AlphaStormStateTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();

        // Inactive initially
        assert !AlphaStormState.isActive(mem);
        assert AlphaStormState.intensity(mem, 0) == null;

        // Start at day 0
        AlphaStormState.start(mem, 0);
        assert AlphaStormState.isActive(mem);
        assert AlphaStormState.intensity(mem, 0) == Intensity.MILD;
        assert AlphaStormState.intensity(mem, 2) == Intensity.MILD;
        assert AlphaStormState.intensity(mem, 3) == Intensity.MODERATE;
        assert AlphaStormState.intensity(mem, 4) == Intensity.MODERATE;
        assert AlphaStormState.intensity(mem, 5) == Intensity.SEVERE;
        assert AlphaStormState.intensity(mem, 100) == Intensity.SEVERE;

        // daysActive
        assert AlphaStormState.daysActive(mem, 7) == 7;

        // Daily costs
        assert AlphaStormState.dailyCost(Intensity.MILD) == 2;
        assert AlphaStormState.dailyCost(Intensity.MODERATE) == 5;
        assert AlphaStormState.dailyCost(Intensity.SEVERE) == 10;

        // Stop
        AlphaStormState.stop(mem);
        assert !AlphaStormState.isActive(mem);
        assert AlphaStormState.intensity(mem, 0) == null;

        System.out.println("ALL PASSED");
    }
}
