package skills_overhaul.artificer;

public class ECMTrackerTest {
    public static void main(String[] args) throws Exception {
        // ratingForFleet: 1.5% per combat ship + 3% per automated ship
        assert Math.abs(ECMTracker.ratingForFleet(0, 0) - 0f) < 1e-4f;
        assert Math.abs(ECMTracker.ratingForFleet(10, 0) - 0.15f) < 1e-4f;
        assert Math.abs(ECMTracker.ratingForFleet(10, 5) - 0.30f) < 1e-4f : "+10*0.015 + 5*0.030 = 0.30";

        // advantage
        assert Math.abs(ECMTracker.advantage(0.30f, 0.20f) - 0.10f) < 1e-4f;
        assert Math.abs(ECMTracker.advantage(0.10f, 0.30f) - (-0.20f)) < 1e-4f;

        // weapon range penalty: 1% per 1% advantage, cap 10%
        assert Math.abs(ECMTracker.enemyWeaponRangePenaltyMult(0.05f) - 0.95f) < 1e-4f;
        assert Math.abs(ECMTracker.enemyWeaponRangePenaltyMult(0.15f) - 0.90f) < 1e-4f : "cap at 10%";
        assert Math.abs(ECMTracker.enemyWeaponRangePenaltyMult(-0.05f) - 1.0f) < 1e-4f : "no penalty when behind";

        System.out.println("ALL PASSED");
    }
}
