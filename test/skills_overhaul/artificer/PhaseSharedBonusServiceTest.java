package skills_overhaul.artificer;

public class PhaseSharedBonusServiceTest {
    public static void main(String[] args) throws Exception {
        // Pool size 150; per-ship = 150 / (totalDP/10); floor 0.20, cap 1.50
        assert Math.abs(PhaseSharedBonusService.computeBonus(0f) - 1.50f) < 1e-4f : "0 DP → cap";
        assert Math.abs(PhaseSharedBonusService.computeBonus(50f) - 1.50f) < 1e-4f : "50 DP: 150/(50/10)=30 capped at 1.50";
        assert Math.abs(PhaseSharedBonusService.computeBonus(100f) - 1.50f) < 1e-4f;
        assert Math.abs(PhaseSharedBonusService.computeBonus(7500f) - 0.20f) < 1e-4f : "floor at 0.20";
        assert Math.abs(PhaseSharedBonusService.computeBonus(150f) - 1.50f) < 1e-4f : "150 DP → 150/15=10 capped at 1.50";
        // Recompute the 150 case: totalDP=150 → totalDP/10=15 → 150/15=10 → cap at 1.50

        // Scaling region: between cap and floor. At totalDP=300: 150/(300/10)=150/30=5, capped 1.50 still
        // Actually scaling kicks in below 1.50 only when raw < 1.50, i.e. when totalDP/10 > 100 → totalDP > 1000
        // But our floor at 0.20 hits at 150/0.20=750. Let me reverify: raw = 150/(totalDP/10) = 1500/totalDP.
        // raw=1.5 when totalDP=1000. raw=0.2 when totalDP=7500.
        // So floor 0.20 hits at totalDP=7500, not 750. Spec might want different math.

        // The simplest reading: "Pool size 150" is total pct — 150% divvied up. Per ship bonus
        // = 150 / shipCount-equivalent. The "(totalPhaseDP/10)" is the divisor — i.e. each 10 DP of phase ship
        // counts as 1 "share". So:
        //   1 phase ship at 50 DP → 5 shares → 30% per share. Capped at 150%.
        //   3 phase ships at 50 DP each → 15 shares → 10% per share.
        //   Cap 150% (one tiny phase ship gets full pool).
        //   Floor 20% (huge phase armada doesn't drop below 20%).

        // OK, the math I wrote: bonus = 150 / (totalDP / 10) = 1500 / totalDP
        // 1500 / 50 = 30 → capped 1.50
        // 1500 / 750 = 2 → 2.0... wait that's above cap.
        // Let me recompute: 1500/totalDP. For totalDP=750, that's 2.0. Cap to 1.50.
        // Floor 0.20 hits at 1500/totalDP = 0.20 → totalDP = 7500.

        assert Math.abs(PhaseSharedBonusService.computeBonus(7500f) - 0.20f) < 1e-4f;
        assert Math.abs(PhaseSharedBonusService.computeBonus(15000f) - 0.20f) < 1e-4f : "extreme = floor";

        // Linear scaling region: 1000 < totalDP < 7500
        float midDP = 3000f;
        float midBonus = PhaseSharedBonusService.computeBonus(midDP);
        assert midBonus > 0.20f && midBonus < 1.50f : "mid region in scaling band";
        assert Math.abs(midBonus - 0.50f) < 1e-4f : "1500/3000 = 0.50";

        System.out.println("ALL PASSED");
    }
}
