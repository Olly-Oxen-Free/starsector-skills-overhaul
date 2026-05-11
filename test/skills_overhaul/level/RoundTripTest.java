package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

/**
 * End-to-end round-trip: XP gain → level-up grants points → spend on
 * node → respec → re-spend. Catches integration regressions across
 * SkillMemory / XPCurve / LevelService / SpendService / RespecService.
 */
public class RoundTripTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<String,Object>();

        // 1. XP accrual + level reconcile
        long xpForLevel3 = XPCurve.cumulativeXP(3);
        SkillMemory.addXP(mem, Tree.PILOT, xpForLevel3);
        LevelService.Cfg cfg = new LevelService.Cfg(1, 1);
        LevelService.reconcileFromXP(mem, Tree.PILOT, xpForLevel3, cfg);
        assert SkillMemory.getLevel(mem, Tree.PILOT) == 3 : "level 3 from XP";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 3
                : "3 points granted across 3 level-ups (no bonus at L5)";

        // 2. Spend on real node
        SpendService.Result r1 = SpendService.spend(mem, Tree.PILOT, "so_defense_fortify", 1);
        assert r1.success : "spend success";
        assert r1.fromTreeLocked == 1 : "took 1 tree-locked";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 2 : "2 points remain";
        RespecService.recordPurchase(mem, "so_defense_fortify", r1.fromTreeLocked, r1.fromFreeUni,
                r1.fromMigrationToPilot + r1.fromMigrationToCommander + r1.fromMigrationToAdmin);
        assert Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey("so_defense_fortify")))
                : "purchased flag set";

        // 3. Respec → point returned
        RespecService.Result rr = RespecService.respecTree(mem, Tree.PILOT);
        assert rr.storyPointsCharged == 0 : "first respec free";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 3
                : "all 3 points back (was: " + SkillMemory.getPoints(mem, Tree.PILOT) + ")";
        assert !Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey("so_defense_fortify")))
                : "purchased flag cleared on respec";

        // 4. Re-spend on different node — flows still work
        SpendService.Result r2 = SpendService.spend(mem, Tree.PILOT, "so_maneuver_drift_instinct", 1);
        assert r2.success : "re-spend success";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 2 : "2 points remain after re-spend";

        // 5. Second respec costs 1 SP
        RespecService.recordPurchase(mem, "so_maneuver_drift_instinct", r2.fromTreeLocked, 0, 0);
        RespecService.Result rr2 = RespecService.respecTree(mem, Tree.PILOT);
        assert rr2.storyPointsCharged == 1 : "second respec charges 1 SP";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 3 : "still 3 points after second respec";

        System.out.println("ALL PASSED");
    }
}
