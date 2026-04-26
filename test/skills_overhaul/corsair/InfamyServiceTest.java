package skills_overhaul.corsair;

import java.util.HashMap;
import java.util.Map;

public class InfamyServiceTest {
    public static void main(String[] args) throws Exception {
        // --- recordStacks accumulates into today's bucket ---
        Map<String,Object> mem = new HashMap<>();
        InfamyService.recordStacks(mem, 100, 3);
        InfamyService.recordStacks(mem, 100, 4);
        assert InfamyService.getInfamy(mem, 100) == 7 : "today's bucket sums adds";

        InfamyService.recordStacks(mem, 101, 2);
        assert InfamyService.getInfamy(mem, 101) == 9 : "yesterday(7) + today(2)";

        // 30-day rollover
        Map<String,Object> mem2 = new HashMap<>();
        InfamyService.recordStacks(mem2, 0, 5);
        assert InfamyService.getInfamy(mem2, 30) == 0 : "day-30 sees no day-0 stacks";

        // gap > 30
        Map<String,Object> mem3 = new HashMap<>();
        InfamyService.recordStacks(mem3, 0, 8);
        InfamyService.recordStacks(mem3, 5, 8);
        assert InfamyService.getInfamy(mem3, 100) == 0 : "gap > 30 → window cleared";

        // up to 10 stacks per call accepted
        Map<String,Object> mem4 = new HashMap<>();
        InfamyService.recordStacks(mem4, 50, 10);
        assert InfamyService.getInfamy(mem4, 50) == 10;

        // tier helpers
        assert InfamyTier.fromInfamy(0) == null;
        assert InfamyTier.fromInfamy(5) == InfamyTier.KNOWN;
        assert InfamyTier.fromInfamy(9) == InfamyTier.KNOWN;
        assert InfamyTier.fromInfamy(10) == InfamyTier.WANTED;
        assert InfamyTier.fromInfamy(14) == InfamyTier.WANTED;
        assert InfamyTier.fromInfamy(15) == InfamyTier.NOTORIOUS;
        assert InfamyTier.fromInfamy(50) == InfamyTier.DREAD;
        assert InfamyTier.fromInfamy(100) == InfamyTier.DREAD;

        System.out.println("ALL PASSED");
    }
}
