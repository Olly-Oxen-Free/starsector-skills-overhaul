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

        System.out.println("ALL PASSED");
    }
}
