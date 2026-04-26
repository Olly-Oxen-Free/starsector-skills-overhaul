package skills_overhaul.commandant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FieldCommandsServiceTest {
    public static void main(String[] args) throws Exception {
        // diff
        Set<String> last = new HashSet<>();
        Set<String> now = new HashSet<>(Arrays.asList("a:1:m1", "a:1:m2"));
        Set<String> newly = FieldCommandsService.diffNewlyIssued(last, now);
        assert newly.size() == 2;

        last = new HashSet<>(Arrays.asList("a:1:m1"));
        now = new HashSet<>(Arrays.asList("a:1:m1", "a:1:m2", "b:2:m3"));
        newly = FieldCommandsService.diffNewlyIssued(last, now);
        assert newly.size() == 2 && newly.contains("a:1:m2") && newly.contains("b:2:m3");

        // tickBuffTimers
        java.util.Map<String, Float> buffs = new java.util.HashMap<>();
        buffs.put("a:1:m1", 5f);
        buffs.put("b:2:m2", 0.5f);
        Set<String> expired = FieldCommandsService.tickBuffTimers(buffs, 1f);
        assert expired.size() == 1 && expired.contains("b:2:m2");
        assert buffs.size() == 1 && buffs.containsKey("a:1:m1");
        assert Math.abs(buffs.get("a:1:m1") - 4f) < 1e-4f;

        // canApplyRetreat
        Set<String> used = new HashSet<>();
        assert FieldCommandsService.canApplyRetreat(used, "ship_x");
        used.add("ship_x");
        assert !FieldCommandsService.canApplyRetreat(used, "ship_x");
        assert FieldCommandsService.canApplyRetreat(used, "ship_y");

        // bufferDurationFor
        assert FieldCommandsService.bufferDurationFor("ASSAULT") == 20f;
        assert FieldCommandsService.bufferDurationFor("RETREAT") == 10f;
        assert FieldCommandsService.bufferDurationFor("RALLY_TASK_FORCE") == 30f;
        assert FieldCommandsService.bufferDurationFor("STRIKE") == 20f;
        assert FieldCommandsService.bufferDurationFor("LIGHT_ESCORT") == 20f;
        assert FieldCommandsService.bufferDurationFor("UNKNOWN_TYPE") == 0f;

        System.out.println("ALL PASSED");
    }
}
