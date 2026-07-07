package skills_overhaul.zealot;

import java.util.HashMap;
import java.util.Map;

public class ZealServiceTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();
        assert ZealService.getStacks(mem) == 0;
        assert ZealService.getCap(mem) == ZealService.BASE_CAP;

        ZealService.addStacks(mem, 30, 0);
        assert ZealService.getStacks(mem) == 30;
        ZealService.addStacks(mem, 80, 0);
        assert ZealService.getStacks(mem) == 100 : "capped at 100";

        ZealService.registerCapBonus(mem, "avenge", 20);
        assert ZealService.getCap(mem) == 120;
        ZealService.addStacks(mem, 25, 0);
        assert ZealService.getStacks(mem) == 120 : "raised cap allows more";

        // Crusade cap bonus stacks with other registered bonuses, and unregister removes it.
        ZealService.registerCapBonus(mem, "crusade", 50);
        assert ZealService.getCap(mem) == 170 : "cap bonuses sum (base + avenge + crusade)";
        ZealService.unregisterCapBonus(mem, "crusade");
        assert ZealService.getCap(mem) == 120 : "unregister removes crusade bonus";
        ZealService.unregisterCapBonus(mem, "avenge");
        assert ZealService.getCap(mem) == ZealService.BASE_CAP : "unregister all → base cap";

        // Pause condition: gained-in-last-24h
        Map<String,Object> mem2 = new HashMap<>();
        ZealService.addStacks(mem2, 50, 0);
        ZealService.advanceDay(mem2, 1, 0f);
        assert ZealService.getStacks(mem2) == 50 : "no decay within 24h of gain";

        // Decay over multiple days; at 50 stacks: (1.5 + 0.02 × 50) = 2.5/day
        ZealService.advanceDay(mem2, 5, 0f);
        assert ZealService.getStacks(mem2) <= 41 && ZealService.getStacks(mem2) >= 39
                : "expected ~40 after 4 days of decay, got " + ZealService.getStacks(mem2);

        // Singularity reduction: -30% of decay
        Map<String,Object> mem3 = new HashMap<>();
        ZealService.addStacks(mem3, 50, 0);
        ZealService.advanceDay(mem3, 5, 0.30f);
        assert ZealService.getStacks(mem3) >= 42 && ZealService.getStacks(mem3) <= 44
                : "reduction case expected ~43, got " + ZealService.getStacks(mem3);

        System.out.println("ALL PASSED");
    }
}
