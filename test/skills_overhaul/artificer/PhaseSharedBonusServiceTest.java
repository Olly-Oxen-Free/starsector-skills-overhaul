package skills_overhaul.artificer;

public class PhaseSharedBonusServiceTest {
    public static void main(String[] args) throws Exception {
        // Pool of 150 percentage points shared per 10 DP of deployed phase ships:
        // per-ship bonus = (150 / (totalDP / 10)) / 100, floor 0.20, cap 1.50.
        assert Math.abs(PhaseSharedBonusService.computeBonus(0f) - 1.50f) < 1e-4f : "0 DP → cap";
        assert Math.abs(PhaseSharedBonusService.computeBonus(10f) - 1.50f) < 1e-4f : "10 DP: 1 share → 150% (cap)";
        assert Math.abs(PhaseSharedBonusService.computeBonus(50f) - 0.30f) < 1e-4f : "50 DP: 5 shares → 30%";
        assert Math.abs(PhaseSharedBonusService.computeBonus(100f) - 0.20f) < 1e-4f : "100 DP: 10 shares → 15% → floor 20%";
        assert Math.abs(PhaseSharedBonusService.computeBonus(150f) - 0.20f) < 1e-4f : "150 DP: 15 shares → 10% → floor 20%";
        assert Math.abs(PhaseSharedBonusService.computeBonus(30f) - 0.50f) < 1e-4f : "30 DP: 3 shares → 50%";
        assert Math.abs(PhaseSharedBonusService.computeBonus(5f) - 1.50f) < 1e-4f : "5 DP: 300% → cap 150%";
        assert Math.abs(PhaseSharedBonusService.computeBonus(85f) - 0.20f) < 1e-4f : "85 DP: ~17.6% → floor 20%";
        assert Math.abs(PhaseSharedBonusService.computeBonus(15000f) - 0.20f) < 1e-4f : "extreme = floor";

        System.out.println("ALL PASSED");
    }
}
