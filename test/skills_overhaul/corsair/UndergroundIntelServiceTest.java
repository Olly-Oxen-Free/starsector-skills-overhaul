package skills_overhaul.corsair;

public class UndergroundIntelServiceTest {
    public static void main(String[] args) throws Exception {
        // score = cargo + bounty + officer*5000 − abs(fleetFP − playerFP) * 0.5
        assert UndergroundIntelService.score(10000f, 5000f, 2, 100f, 100f) == 25000f
                : "no FP gap; cargo + bounty + 2*5000";
        assert UndergroundIntelService.score(0f, 0f, 0, 100f, 200f) == -50f
                : "FP diff 100, score -50";
        assert UndergroundIntelService.score(1000f, 0f, 0, 100f, 50f) == 1000f - 25f
                : "FP diff 50, half-penalty 25";

        // strength bands
        assert UndergroundIntelService.inStrengthBand(100f, 100f, UndergroundIntelService.Strength.EVEN);
        assert !UndergroundIntelService.inStrengthBand(60f, 100f, UndergroundIntelService.Strength.EVEN);
        assert UndergroundIntelService.inStrengthBand(70f, 100f, UndergroundIntelService.Strength.WEAK);
        assert UndergroundIntelService.inStrengthBand(125f, 100f, UndergroundIntelService.Strength.STRONG);

        // cooldown
        java.util.Map<String, Object> mem = new java.util.HashMap<>();
        assert !UndergroundIntelService.cooldownActive(mem, 0L);
        UndergroundIntelService.setCooldown(mem, 100L);
        assert UndergroundIntelService.cooldownActive(mem, 50L);
        assert !UndergroundIntelService.cooldownActive(mem, 100L);
        assert !UndergroundIntelService.cooldownActive(mem, 200L);

        System.out.println("ALL PASSED");
    }
}
