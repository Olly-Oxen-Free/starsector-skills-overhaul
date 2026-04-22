package skills_overhaul.level;

public class XPCurveTest {
    public static void main(String[] args) {
        // Curve: xp_for_level(n) = 500 * n^1.35  (cheaper than vanilla ~1000*n^1.5)
        assert Math.abs(XPCurve.xpForLevel(1) - 500) < 1 : "L1";
        long l5 = XPCurve.xpForLevel(5);
        assert l5 > 500 && l5 < 10000 : "L5 magnitude";

        // Cumulative xp from 1 to 3 = xp(1)+xp(2)+xp(3)
        long cum = XPCurve.cumulativeXP(3);
        long sum = XPCurve.xpForLevel(1) + XPCurve.xpForLevel(2) + XPCurve.xpForLevel(3);
        assert cum == sum : "cumulative == sum";

        // levelFromXP is monotonic and inverse
        assert XPCurve.levelFromXP(0) == 0 : "L0 at zero";
        assert XPCurve.levelFromXP(cum) == 3 : "exactly L3";
        assert XPCurve.levelFromXP(cum - 1) == 2 : "just under L3";

        // Cap at 25
        long huge = 10_000_000L;
        assert XPCurve.levelFromXP(huge) == 25 : "cap 25";

        System.out.println("ALL PASSED");
    }
}
