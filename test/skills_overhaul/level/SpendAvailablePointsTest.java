package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

/** Review 2026-11-02-0039-ui B2: the UI budget must honour the migration-pool lock. */
public class SpendAvailablePointsTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<String,Object>();
        SkillMemory.setPoints(mem, Tree.COMMANDER, 1);
        SkillMemory.setPoints(mem, Tree.PILOT, 0);
        mem.put(SpendService.POOL_UNALLOC, 3f);
        mem.put(SpendService.POOL_LOCK_KEY, Tree.PILOT.name());

        assert SpendService.availablePoints(mem, Tree.COMMANDER) == 1
                : "pool locked to PILOT is not available to COMMANDER";
        assert SpendService.availablePoints(mem, Tree.PILOT) == 3 : "pool available to its own tree";

        mem.remove(SpendService.POOL_LOCK_KEY);
        mem.put(SkillMemory.freeUniKey(), 2f);
        mem.put(SkillMemory.migUniKey(Tree.COMMANDER), 1f);
        assert SpendService.availablePoints(mem, Tree.COMMANDER) == 1 + 1 + 3 + 2 : "all pools when unlocked";

        // Agrees with spend: with the pool locked elsewhere, COMMANDER can spend exactly 1.
        Map<String,Object> m2 = new HashMap<String,Object>();
        SkillMemory.setPoints(m2, Tree.COMMANDER, 1);
        m2.put(SpendService.POOL_UNALLOC, 3f);
        m2.put(SpendService.POOL_LOCK_KEY, Tree.PILOT.name());
        int avail = SpendService.availablePoints(m2, Tree.COMMANDER);
        assert !SpendService.spend(new HashMap<String,Object>(m2), Tree.COMMANDER, "a", avail + 1).success;
        assert SpendService.spend(new HashMap<String,Object>(m2), Tree.COMMANDER, "a", avail).success;

        System.out.println("ALL PASSED");
    }
}
