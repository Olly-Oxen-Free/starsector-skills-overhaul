package skills_overhaul.commandant;

public class CommandFieldServiceTest {
    public static void main(String[] args) throws Exception {
        // Reset state first (in case prior tests in same JVM run leaked)
        CommandFieldService.unregisterParam("a");
        CommandFieldService.unregisterParam("b");
        CommandFieldService.unregisterParam("officer_training");
        CommandFieldService.unregisterParam("esprit");

        // --- param registry ---
        CommandFieldService.registerParam("a", "fleetCapBonus", 0.02f);
        CommandFieldService.registerParam("b", "fleetCapBonus", 0.05f);
        assert Math.abs(CommandFieldService.sumParam("fleetCapBonus") - 0.07f) < 1e-4f;
        CommandFieldService.unregisterParam("a");
        assert Math.abs(CommandFieldService.sumParam("fleetCapBonus") - 0.05f) < 1e-4f;
        CommandFieldService.unregisterParam("b");
        assert CommandFieldService.sumParam("fleetCapBonus") == 0f;

        // --- range computation ---
        CommandFieldService.registerParam("officer_training", "rangeBumpFlat", 250f);
        assert CommandFieldService.computeEffectiveRange(1) == 1750f;  // 1=FRIGATE
        CommandFieldService.unregisterParam("officer_training");

        // --- per-class range multiplier ---
        CommandFieldService.registerParam("esprit", "rangeBumpMultByClass:CAPITAL_SHIP", 1.333f);
        float capRange = CommandFieldService.computeEffectiveRange(4);  // 4=CAPITAL_SHIP
        assert capRange > 1990f && capRange < 2010f : "capital × 1.333 ≈ 2000, got " + capRange;
        assert CommandFieldService.computeEffectiveRange(1) == 1500f;
        CommandFieldService.unregisterParam("esprit");

        // --- totals: officer at level N gives N × 0.01 (cap 0.06 per officer) ---
        float[] totals = CommandFieldService.computeTotals(
                new int[]{3, 3}, new boolean[]{false, false}, 1f, 0f);
        assert Math.abs(totals[0] - 0.06f) < 1e-4f : "level3+level3 = 6%, capped at fleet 12% but each officer caps at 6% so sum=6%; wait — sum is 0.03+0.03=0.06";

        // 3 officered ships at level 6 each = 0.18 raw, capped at 0.12 fleet cap
        totals = CommandFieldService.computeTotals(
                new int[]{6, 6, 6}, new boolean[]{false, false, false}, 1f, 0f);
        assert Math.abs(totals[0] - 0.12f) < 1e-4f : "fleet cap +12%";

        // With Iron Doctrine fleetCapBonus +0.08, cap = 0.20
        totals = CommandFieldService.computeTotals(
                new int[]{6, 6, 6}, new boolean[]{false, false, false}, 1f, 0.08f);
        assert Math.abs(totals[0] - 0.20f) < 1e-4f : "fleet cap +20%";

        // AI core officers at 75% effectiveness
        totals = CommandFieldService.computeTotals(
                new int[]{6}, new boolean[]{true}, 1f, 0f);
        assert Math.abs(totals[0] - 0.045f) < 1e-4f : "AI core L6 = 0.06 × 0.75 = 0.045";

        // perLevelScalingMult = 1.5 (Iron Doctrine Entrenched amplification)
        totals = CommandFieldService.computeTotals(
                new int[]{6}, new boolean[]{false}, 1.5f, 0f);
        assert Math.abs(totals[0] - 0.09f) < 1e-4f : "L6 × 1.5 = 0.09";

        System.out.println("ALL PASSED");
    }
}
