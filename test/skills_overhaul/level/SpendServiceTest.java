package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

public class SpendServiceTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<>();
        SkillMemory.addPoints(mem, Tree.PILOT, 2);

        // Spend 1 point from tree-locked
        SpendService.Result r = SpendService.spend(mem, Tree.PILOT, "so_pilot_root_1", 1);
        assert r.success : "spend success";
        assert r.fromTreeLocked == 1 : "1 from tree-locked";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 1 : "bank 1 left";

        // Spend beyond tree-locked: consume 1 tree-locked + free universal
        mem.put(SkillMemory.freeUniKey(), 1f);
        r = SpendService.spend(mem, Tree.PILOT, "so_pilot_root_2", 2);
        assert r.success : "composite spend";
        assert r.fromTreeLocked == 1 && r.fromFreeUni == 1 : "1/1 split";

        // Spend with migrated uni: locks to tree
        mem.put("$skills.points.universal_migrated_unallocated", 3f);
        SkillMemory.addPoints(mem, Tree.PILOT, 0);
        r = SpendService.spend(mem, Tree.PILOT, "so_pilot_gunner_1", 2);
        assert r.success : "mig spend";
        assert r.fromMigrationToPilot == 2 : "2 from migration -> pilot";
        // universal_migrated_unallocated drained
        Object pool = mem.get("$skills.points.universal_migrated_unallocated");
        assert ((Number)pool).floatValue() == 1f : "pool 1 left, locked to pilot next time";
        // now migration is locked to pilot; attempting to spend in commander from pool should fail
        r = SpendService.spend(mem, Tree.COMMANDER, "so_cmd_root_1", 1);
        assert !r.success : "can't cross-tree after lock";

        // Refund path: spend then refund restores state
        Map<String,Object> rmem = new HashMap<>();
        SkillMemory.addPoints(rmem, Tree.PILOT, 5);
        SpendService.Result s1 = SpendService.spend(rmem, Tree.PILOT, "node_x", 3);
        assert s1.success && SkillMemory.getPoints(rmem, Tree.PILOT) == 2 : "pre-refund spend";
        assert Boolean.TRUE.equals(rmem.get(SkillMemory.purchasedKey("node_x"))) : "purchased flag set";

        SpendService.Result s2 = SpendService.refund(rmem, Tree.PILOT, "node_x", 3, 3, 0, 0);
        assert s2.success : "refund success";
        assert SkillMemory.getPoints(rmem, Tree.PILOT) == 5 : "points restored";
        assert !rmem.containsKey(SkillMemory.purchasedKey("node_x")) : "purchased flag cleared";

        // Pool lock clears after full migration-tree-lock refund
        Map<String,Object> lmem = new HashMap<>();
        lmem.put("$skills.points.universal_migrated_unallocated", 3f);
        SpendService.spend(lmem, Tree.PILOT, "pool_node", 2);
        assert "PILOT".equals(lmem.get("$skills.points.universal_migrated_locked_to")) : "lock set to PILOT";
        SpendService.refund(lmem, Tree.PILOT, "pool_node", 2, 0, 0, 2);
        assert !lmem.containsKey("$skills.points.universal_migrated_locked_to") : "lock cleared after migUni→0";

        // Failed spend leaves the map unchanged
        Map<String,Object> fmem = new HashMap<>();
        SkillMemory.addPoints(fmem, Tree.PILOT, 1);
        fmem.put(SkillMemory.freeUniKey(), 1f);
        fmem.put("$skills.points.universal_migrated_unallocated", 2f);
        fmem.put("$skills.points.universal_migrated_locked_to", "COMMANDER");
        Map<String,Object> fbefore = new HashMap<>(fmem);
        SpendService.Result fr = SpendService.spend(fmem, Tree.PILOT, "too_dear", 5);
        assert !fr.success : "spend of 5 with 2 usable fails";
        assert fr.reason.equals("not enough points (short 3)") : fr.reason;
        assert fmem.equals(fbefore) : "failed spend must not touch the map";

        System.out.println("ALL PASSED");
    }
}
