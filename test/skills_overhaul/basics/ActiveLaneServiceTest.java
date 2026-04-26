package skills_overhaul.basics;

import skills_overhaul.state.SkillMemory;

import java.util.*;

public class ActiveLaneServiceTest {
    public static void main(String[] args) throws Exception {
        // --- onPurchased: first 2 auto-add ---
        Map<String,Object> mem = new HashMap<>();
        ActiveLaneService.onPurchased(mem, "t0_a");
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_a"));
        ActiveLaneService.onPurchased(mem, "t0_b");
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_a", "t0_b"));

        // 3rd T0 stays purchased but inactive
        ActiveLaneService.onPurchased(mem, "t0_c");
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_a", "t0_b"))
                : "3rd T0 not auto-added";
        assert !ActiveLaneService.isActive(mem, "t0_c");
        assert ActiveLaneService.isActive(mem, "t0_a");
        assert ActiveLaneService.isActive(mem, "t0_b");

        // --- canSwap: out-of-combat true; in-combat false ---
        assert ActiveLaneService.canSwap(false);
        assert !ActiveLaneService.canSwap(true);

        // --- swap: removes oldId, adds newId; preserves order with newId taking oldId's slot ---
        boolean ok = ActiveLaneService.swap(mem, "t0_a", "t0_c");
        assert ok;
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_c", "t0_b"));
        assert !ActiveLaneService.isActive(mem, "t0_a");
        assert ActiveLaneService.isActive(mem, "t0_c");

        // --- swap: oldId not active -> reject ---
        assert !ActiveLaneService.swap(mem, "ghost", "t0_a");

        // --- swap: newId already active -> reject (no duplicates) ---
        assert !ActiveLaneService.swap(mem, "t0_b", "t0_c");

        // --- onUnpurchased: removes from active set if present ---
        ActiveLaneService.onUnpurchased(mem, "t0_b");
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_c"));
        // unpurchasing a non-active is a no-op
        ActiveLaneService.onUnpurchased(mem, "t0_b");
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_c"));

        // --- add fills empty slot ---
        boolean added = ActiveLaneService.add(mem, "t0_a");
        assert added;
        assert ActiveLaneService.getActive(mem).equals(Arrays.asList("t0_c", "t0_a"));
        // full -> reject
        boolean addedFull = ActiveLaneService.add(mem, "t0_b");
        assert !addedFull;

        System.out.println("ALL PASSED");
    }
}
