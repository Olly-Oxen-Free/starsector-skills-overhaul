package skills_overhaul.pilot;

/**
 * Regression for review 2026-11-02-0835-pilot B4: Flanking, Knife Fighter,
 * Dragon Slayer, Point Blank and Dead Eye only evaluated their condition while
 * the flagship had a target, so the unmodify branch never ran once the target
 * was cleared and the buff stayed on for the rest of the battle. The lanes now
 * evaluate these every frame; no target must mean "inactive".
 */
public class TargetGatedConditionsTest {
    public static void main(String[] args) {
        // No target: every condition is off, whatever the stale measurement says.
        assert !TargetGatedConditions.knifeFighter(false, 0f) : "knife fighter needs a target";
        assert !TargetGatedConditions.flanking(false, 180f) : "flanking needs a target";
        assert TargetGatedConditions.dragonSlayerBonus(false, 3) == 0f : "dragon slayer needs a target";
        assert !TargetGatedConditions.pointBlank(false, 0f) : "point blank needs a target";
        assert !TargetGatedConditions.deadEyeFluxDiscount(false, 2000f) : "dead eye discount needs a target";

        // With a target, the existing thresholds are unchanged.
        assert TargetGatedConditions.knifeFighter(true, 300f);
        assert !TargetGatedConditions.knifeFighter(true, 301f);
        assert TargetGatedConditions.flanking(true, 46f);
        assert !TargetGatedConditions.flanking(true, 45f);
        assert Math.abs(TargetGatedConditions.dragonSlayerBonus(true, 1) - 0.10f) < 1e-6f;
        assert Math.abs(TargetGatedConditions.dragonSlayerBonus(true, 5) - 0.30f) < 1e-6f : "capped at 30%";
        assert TargetGatedConditions.dragonSlayerBonus(true, -1) == 0f : "no bonus vs smaller hulls";
        assert TargetGatedConditions.pointBlank(true, 400f);
        assert !TargetGatedConditions.pointBlank(true, 401f);
        assert TargetGatedConditions.deadEyeFluxDiscount(true, 751f);
        assert !TargetGatedConditions.deadEyeFluxDiscount(true, 750f);

        System.out.println("ALL PASSED");
    }
}
