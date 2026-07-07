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

        // Effective per-ship contribution multiplier that makes the fixed vanilla
        // threshold behave like the raised cap (VANILLA_BASE / raisedCap).
        AutomatedShipPointService.clearForTest();
        assert Math.abs(AutomatedShipCapApplier.getEffectiveMult() - 1.0f) < 1e-6
                : "no bonus → mult 1.0";

        AutomatedShipPointService.registerBonus("auto_deploy", 40); // cap 160
        assert Math.abs(AutomatedShipCapApplier.getEffectiveMult() - (120f / 160f)) < 1e-6
                : "cap 160 → mult 0.75";

        AutomatedShipPointService.registerBonus("limit_breaker", 40); // cap 200
        assert Math.abs(AutomatedShipCapApplier.getEffectiveMult() - (120f / 200f)) < 1e-6
                : "cap 200 → mult 0.60";

        AutomatedShipPointService.clearForTest();

        System.out.println("ALL PASSED");
    }
}
