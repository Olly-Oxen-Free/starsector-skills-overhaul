package skills_overhaul.xp;

public class PilotXPMathTest {
    public static void main(String[] args) {
        assert Math.abs(PilotXPMath.weightedDealt(0.5f, 0.5f, 0.40f) - 0.70f) < 1e-4 : "weighted 1";
        assert Math.abs(PilotXPMath.weightedDealt(0.9f, 0.5f, 0.40f) - 1.00f) < 1e-4 : "weighted clamp 1";
        assert Math.abs(PilotXPMath.weightedDealt(0f, 0f, 0.40f) - 0f) < 1e-6 : "weighted zero";

        assert Math.abs(PilotXPMath.underdog(10f, 10f, 0.75f, 2.0f) - 1.00f) < 1e-4 : "underdog 1";
        assert Math.abs(PilotXPMath.underdog(40f, 10f, 0.75f, 2.0f) - 2.00f) < 1e-4 : "underdog cap hi";
        assert Math.abs(PilotXPMath.underdog(1f, 10f,  0.75f, 2.0f) - 0.75f) < 1e-4 : "underdog cap lo";

        assert Math.abs(PilotXPMath.killBonus(0.20f, 0.15f, 0.20f) - 0.20f) < 1e-4 : "kb granted";
        assert PilotXPMath.killBonus(0.10f, 0.15f, 0.20f) == 0f : "kb denied";

        float r = PilotXPMath.shipReward(100f, 0.30f, 0.10f, 20f, 10f, PilotXPMath.cfgDefault());
        // weighted = 0.30 + 0.04 = 0.34; share = 0.34+0.20 = 0.54; underdog = 2.0 → 100*0.54*2.0 = 108
        assert Math.abs(r - 108f) < 1e-2 : "shipReward composed: got " + r;

        float s = PilotXPMath.survivalBonus(true, true, 500f, 20f, 40f, 30f, 15f, PilotXPMath.cfgDefault());
        assert s == 0f : "survival zero when first ship died";

        s = PilotXPMath.survivalBonus(false, true, 500f, 0f, 60f, 15f, 15f, PilotXPMath.cfgDefault());
        // difficulty=1.0; eff_sec=60; time_frac=1.0 → 0.20*500*1.0*1.0 = 100
        assert Math.abs(s - 100f) < 1e-2 : "survival full: got " + s;

        s = PilotXPMath.survivalBonus(false, true, 500f, 30f, 0f, 15f, 15f, PilotXPMath.cfgDefault());
        // eff_sec = 0.80*30 = 24; time_frac = 24/60 = 0.4 → 0.20*500*0.4*1.0 = 40
        assert Math.abs(s - 0.20f * 500f * (24f/60f) * 1.0f) < 1e-2 : "auto time_frac";

        System.out.println("ALL PASSED");
    }
}
