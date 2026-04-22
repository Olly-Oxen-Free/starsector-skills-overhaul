package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

public class LevelServiceTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<>();

        // Config: 1 per level, +1 every 5th level
        LevelService.Cfg cfg = new LevelService.Cfg(1, 1);

        // Level up from 0 to 1 → +1 point
        int grant = LevelService.processLevelUp(mem, Tree.PILOT, 1, cfg);
        assert grant == 1 : "L1 grants 1";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 1 : "bank 1";

        // Level up from 4 to 5 → +1 regular + 1 bonus = 2
        grant = LevelService.processLevelUp(mem, Tree.PILOT, 5, cfg);
        assert grant == 2 : "L5 grants 2 (1 regular + 1 bonus)";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 3 : "bank 3";

        // Jumping 2 levels (6→8) grants 2 regular
        grant = LevelService.processLevelUp(mem, Tree.PILOT, 6, cfg);
        grant += LevelService.processLevelUp(mem, Tree.PILOT, 7, cfg);
        grant += LevelService.processLevelUp(mem, Tree.PILOT, 8, cfg);
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 6 : "bank 6 after 3 levels";

        // Recompute from XP end-to-end
        long xp = XPCurve.cumulativeXP(5);
        LevelService.reconcileFromXP(mem, Tree.PILOT, xp, cfg);
        assert SkillMemory.getLevel(mem, Tree.PILOT) == 5 : "reconciled level 5";

        System.out.println("ALL PASSED");
    }
}
