package skills_overhaul.xp;

public class AdminXPMathTest {
    public static void main(String[] args) {
        // (a) Founding diminishing pool: n=0 -> BASE; n=1 -> BASE/2; n=4 -> BASE/5
        assert Math.abs(AdminXPMath.foundingXP(0, 1000f) - 1000f) < 0.1 : "founding 0";
        assert Math.abs(AdminXPMath.foundingXP(1, 1000f) - 500f)  < 0.1 : "founding 1";
        assert Math.abs(AdminXPMath.foundingXP(4, 1000f) - 200f)  < 0.1 : "founding 4";

        // (b) Conquest = mil_bonus * size/10 + founding[next_n]
        float cx = AdminXPMath.conquestXP(2, 6, 500f, 1000f);
        // = 500 * 0.6 + 1000/3 = 300 + 333.33 = 633.33
        assert Math.abs(cx - 633.33f) < 0.5 : "conquest";

        // (c) Income: log10(1+credits) * INCOME_RATE; negative income contributes 0
        assert Math.abs(AdminXPMath.incomeXP(99999f, 2.0f) - (float)(Math.log10(1 + 99999f) * 2.0)) < 1e-3 : "income pos";
        assert AdminXPMath.incomeXP(-5000f, 2.0f) == 0f : "income neg floor";
        assert AdminXPMath.incomeXP(0f, 2.0f) == 0f : "income zero";

        // (d) Stability streak: clamp to cap, linear ramp
        assert AdminXPMath.streakXP(0, 6, 1.0f) == 0f : "streak 0";
        assert Math.abs(AdminXPMath.streakXP(3, 6, 10.0f) - 30f) < 0.01 : "streak partial";
        assert Math.abs(AdminXPMath.streakXP(10, 6, 10.0f) - 60f) < 0.01 : "streak capped";

        // (e) Modifier
        assert AdminXPMath.modifier(true,  false) == 1.00f : "self";
        assert AdminXPMath.modifier(false, true)  == 0.25f : "vassal";
        assert AdminXPMath.modifier(false, false) == 0.50f : "admin";
        assert Math.abs(AdminXPMath.modifier(false, true, 0.10f, 0.40f) - 0.10f) < 1e-6 : "vassal override";

        // Streak update: threshold=5, dip tolerance threshold-3=2
        assert AdminXPMath.updateStreak(3, 7, 5) == 4 : "ramp";
        assert AdminXPMath.updateStreak(5, 4, 5) == 5 : "dip tolerated";
        assert AdminXPMath.updateStreak(5, 1, 5) == 0 : "reset";

        System.out.println("ALL PASSED");
    }
}
