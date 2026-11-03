package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

/** Points drawn from the unallocated migration pool are spent once, not also credited to the tree. */
public class SpendServicePoolTest {
    private static final String POOL = "$skills.points.universal_migrated_unallocated";

    public static void main(String[] args) {
        // Pool 5, spend 5 in PILOT: pool drained, nothing left over for PILOT
        Map<String,Object> mem = new HashMap<>();
        mem.put(POOL, 5f);
        SpendService.Result r = SpendService.spend(mem, Tree.PILOT, "so_pilot_a", 5);
        assert r.success && r.fromMigrationToPilot == 5 : "pool spend";
        assert ((Number) mem.get(POOL)).floatValue() == 0f : "pool drained";
        assert ((Number) mem.get(SkillMemory.migUniKey(Tree.PILOT))).floatValue() == 0f
                : "pool points must not be credited to migUni(PILOT): " + mem.get(SkillMemory.migUniKey(Tree.PILOT));
        assert "PILOT".equals(mem.get(SpendService.POOL_LOCK_KEY)) : "pool locked to PILOT";

        RespecService.recordPurchase(mem, "so_pilot_a", r.fromTreeLocked, r.fromFreeUni, r.fromMigrationToPilot);

        SpendService.Result r2 = SpendService.spend(mem, Tree.PILOT, "so_pilot_b", 1);
        assert !r2.success : "second spend has nothing to draw on";

        // Respec round trip: the recorded migration points return to migUni(PILOT) once
        RespecService.respecTree(mem, Tree.PILOT);
        assert ((Number) mem.get(SkillMemory.migUniKey(Tree.PILOT))).floatValue() == 5f
                : "after respec migUni(PILOT) == 5, was " + mem.get(SkillMemory.migUniKey(Tree.PILOT));

        System.out.println("ALL PASSED");
    }
}
