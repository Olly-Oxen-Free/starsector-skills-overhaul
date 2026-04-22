package skills_overhaul.state;

import java.util.HashMap;
import java.util.Map;

public class SkillMemoryTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<>();

        SkillMemory.addXP(mem, Tree.PILOT, 150f);
        SkillMemory.addXP(mem, Tree.PILOT, 50f);
        assert Math.abs(SkillMemory.getXP(mem, Tree.PILOT) - 200f) < 1e-3 : "xp accumulate";
        assert SkillMemory.getXP(mem, Tree.COMMANDER) == 0f : "other trees untouched";

        SkillMemory.setLevel(mem, Tree.ADMIN, 3);
        assert SkillMemory.getLevel(mem, Tree.ADMIN) == 3 : "level";

        SkillMemory.setMigrated(mem, true);
        assert SkillMemory.isMigrated(mem) : "migrated flag";

        SkillMemory.addPoints(mem, Tree.COMMANDER, 2);
        assert SkillMemory.getPoints(mem, Tree.COMMANDER) == 2 : "points";

        System.out.println("ALL PASSED");
    }
}
