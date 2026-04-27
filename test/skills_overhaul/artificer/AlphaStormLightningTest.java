package skills_overhaul.artificer;

import skills_overhaul.artificer.alphastorm.AlphaStormLightning;
import skills_overhaul.artificer.alphastorm.AlphaStormState.Intensity;

public class AlphaStormLightningTest {
    public static void main(String[] args) throws Exception {
        // Strike probability scales with flux level + intensity
        // Low flux + mild = lowest. High flux + severe = highest.
        float low = AlphaStormLightning.probability(0.3f, Intensity.MILD);
        float high = AlphaStormLightning.probability(0.95f, Intensity.SEVERE);
        assert high > low : "high flux + severe should exceed low flux + mild";

        // Damage table
        assert AlphaStormLightning.empDamage(Intensity.MILD) == 100;
        assert AlphaStormLightning.empDamage(Intensity.MODERATE) == 200;
        assert AlphaStormLightning.empDamage(Intensity.SEVERE) == 300;
        assert AlphaStormLightning.energyDamage(Intensity.MILD) == 50;
        assert AlphaStormLightning.energyDamage(Intensity.MODERATE) == 100;
        assert AlphaStormLightning.energyDamage(Intensity.SEVERE) == 150;

        System.out.println("ALL PASSED");
    }
}
