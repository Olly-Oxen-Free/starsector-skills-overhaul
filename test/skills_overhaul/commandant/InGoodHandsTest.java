package skills_overhaul.commandant;

import skills_overhaul.effect.handlers.SOInGoodHandsHandler;

public class InGoodHandsTest {
    public static void main(String[] args) throws Exception {
        assert SOInGoodHandsHandler.reductionForLevel(0f) == 0f;
        assert Math.abs(SOInGoodHandsHandler.reductionForLevel(3f) - 0.05f) < 1e-6f;
        assert Math.abs(SOInGoodHandsHandler.reductionForLevel(6f) - 0.10f) < 1e-6f;
        assert Math.abs(SOInGoodHandsHandler.reductionForLevel(8f) - 0.10f) < 1e-6f; // capped at level 6
        System.out.println("ALL PASSED");
    }
}
