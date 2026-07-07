package skills_overhaul.corsair;

public class PrivateerFleetServiceTest {
    public static void main(String[] args) throws Exception {
        // Fleet FP scaling: scrap_spent * 0.5
        assert PrivateerFleetService.computeFleetFP(100) == 50f;
        assert PrivateerFleetService.computeFleetFP(50) == 25f;
        assert PrivateerFleetService.computeFleetFP(200) == 100f;

        // Min commission gate
        assert !PrivateerFleetService.isCommissionAllowed(49);
        assert PrivateerFleetService.isCommissionAllowed(50);
        assert PrivateerFleetService.isCommissionAllowed(1000);

        // Income split: 30% of victory salvage value
        assert PrivateerFleetService.computeIncomeShare(1000f) == 300f;
        assert PrivateerFleetService.computeIncomeShare(0f) == 0f;

        // Per-battle payout: 30% of destroyed FP * 200 cr/FP, capped at 20k.
        // 100 FP -> 100 * 200 * 0.30 = 6000
        assert PrivateerFleetService.computePrivateerPayout(100f) == 6000f;
        // No destruction -> no payout
        assert PrivateerFleetService.computePrivateerPayout(0f) == 0f;
        assert PrivateerFleetService.computePrivateerPayout(-5f) == 0f;
        // Cap: 1000 FP would be 60000, clamped to 20000
        assert PrivateerFleetService.computePrivateerPayout(1000f) == 20000f;
        // Just below cap boundary: 333 FP -> 19980
        assert PrivateerFleetService.computePrivateerPayout(333f) == 19980f;

        System.out.println("ALL PASSED");
    }
}
