package skills_overhaul.commandant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Re-issuing an order to the same ship must refresh, not stack (reviews/2026-11-03-0435-commandant.md B3). */
public class FieldCommandsReissueTest {
    public static void main(String[] args) throws Exception {
        Map<String, Float> buffs = new HashMap<>();
        Set<String> retreatUsed = new HashSet<>();

        // Cruiser m1 on Strike order A.
        Map<String, String> started = FieldCommandsService.issueBuffs(
                Collections.singleton("STRIKE:111:m1"), buffs, retreatUsed);
        assert started.size() == 1 && "STRIKE".equals(started.values().iterator().next());
        String strikeKey = started.keySet().iterator().next();
        FieldCommandsService.tickBuffTimers(buffs, 5f);

        // Moved to Strike order B within the 20 s window: same buff key, timer refreshed.
        started = FieldCommandsService.issueBuffs(
                Collections.singleton("STRIKE:222:m1"), buffs, retreatUsed);
        assert started.size() == 1 && started.containsKey(strikeKey)
                : "second Strike order stacked a new modifier: " + started.keySet();
        assert buffs.size() == 1 : "one Strike buff per ship, got " + buffs.keySet();
        assert Math.abs(buffs.get(strikeKey) - 20f) < 1e-4f;

        // A different order type on the same ship is a separate buff.
        started = FieldCommandsService.issueBuffs(
                Collections.singleton("ASSAULT:333:m1"), buffs, retreatUsed);
        assert started.size() == 1 && !started.containsKey(strikeKey);
        assert buffs.size() == 2;

        // Same order type on another ship is a separate buff.
        started = FieldCommandsService.issueBuffs(
                Collections.singleton("STRIKE:222:m2"), buffs, retreatUsed);
        assert started.size() == 1 && buffs.size() == 3;

        // Retreat stays once per ship per battle; orders without a buff start nothing.
        started = FieldCommandsService.issueBuffs(
                new HashSet<>(Arrays.asList("RETREAT:444:m3", "UNKNOWN_TYPE:555:m3")), buffs, retreatUsed);
        assert started.size() == 1;
        started = FieldCommandsService.issueBuffs(
                Collections.singleton("RETREAT:666:m3"), buffs, retreatUsed);
        assert started.isEmpty();

        System.out.println("ALL PASSED");
    }
}
