package skills_overhaul.collective;

import skills_overhaul.artificer.AutomatedShipPointService;

public class CollectiveAutomatedPointsTest {
    public static void main(String[] args) {
        AutomatedShipPointService.clearForTest();
        int base = AutomatedShipPointService.getRaisedCap();

        // test 1: T0 raises the live automated-points cap by 60
        CollectiveAutomatedPoints.grantT0();
        assert AutomatedShipPointService.getRaisedCap() == base + 60
                : "Expected " + (base + 60) + " got " + AutomatedShipPointService.getRaisedCap();

        // test 2: Battle Conditioning adds 20 on top
        CollectiveAutomatedPoints.grantBattleConditioning();
        assert AutomatedShipPointService.getRaisedCap() == base + 80;

        // test 3: granting twice does not stack
        CollectiveAutomatedPoints.grantT0();
        assert AutomatedShipPointService.getRaisedCap() == base + 80;

        // test 4: revoking removes each grant
        CollectiveAutomatedPoints.revokeT0();
        assert AutomatedShipPointService.getRaisedCap() == base + 20;
        CollectiveAutomatedPoints.revokeBattleConditioning();
        assert AutomatedShipPointService.getRaisedCap() == base;

        AutomatedShipPointService.clearForTest();
        System.out.println("CollectiveAutomatedPointsTest: all passed.");
    }
}
