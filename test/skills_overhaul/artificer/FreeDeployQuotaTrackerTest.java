package skills_overhaul.artificer;

import skills_overhaul.artificer.forge.FreeDeployQuotaTracker;

public class FreeDeployQuotaTrackerTest {
    public static void main(String[] args) throws Exception {
        FreeDeployQuotaTracker.clearForTest();

        assert FreeDeployQuotaTracker.getQuota() == 1 : "base quota 1";

        FreeDeployQuotaTracker.registerBonus("limit_breaker", 1);
        assert FreeDeployQuotaTracker.getQuota() == 2;

        FreeDeployQuotaTracker.registerBonus("alpha_storm", 1);
        assert FreeDeployQuotaTracker.getQuota() == 3;

        FreeDeployQuotaTracker.unregisterBonus("limit_breaker");
        assert FreeDeployQuotaTracker.getQuota() == 2;

        FreeDeployQuotaTracker.unregisterBonus("alpha_storm");
        assert FreeDeployQuotaTracker.getQuota() == 1;

        System.out.println("ALL PASSED");
    }
}
