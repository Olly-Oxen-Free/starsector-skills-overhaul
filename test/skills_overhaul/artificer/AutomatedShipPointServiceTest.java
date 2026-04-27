package skills_overhaul.artificer;

public class AutomatedShipPointServiceTest {
    public static void main(String[] args) throws Exception {
        AutomatedShipPointService.clearForTest();

        assert AutomatedShipPointService.getRaisedCap() == AutomatedShipPointService.VANILLA_BASE
                : "no bonuses → vanilla base";

        AutomatedShipPointService.registerBonus("auto_deploy", 40);
        assert AutomatedShipPointService.getRaisedCap() == AutomatedShipPointService.VANILLA_BASE + 40;

        AutomatedShipPointService.registerBonus("limit_breaker", 40);
        assert AutomatedShipPointService.getRaisedCap() == AutomatedShipPointService.VANILLA_BASE + 80;

        AutomatedShipPointService.unregisterBonus("auto_deploy");
        assert AutomatedShipPointService.getRaisedCap() == AutomatedShipPointService.VANILLA_BASE + 40;

        // Overcap math (test version takes a deployed-points arg)
        assert AutomatedShipPointService.computeOverCap(140, 120) == 20;
        assert AutomatedShipPointService.computeOverCap(100, 120) == 0;

        System.out.println("ALL PASSED");
    }
}
