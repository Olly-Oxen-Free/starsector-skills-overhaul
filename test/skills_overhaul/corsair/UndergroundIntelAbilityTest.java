package skills_overhaul.corsair;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

public class UndergroundIntelAbilityTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();
        SkillMemory.setLevel(mem, Tree.COMMANDER, 0);

        int today = 100;
        assert costFor(mem, today) == 50f : "no infamy → 50";

        InfamyService.recordStacks(mem, today, 10);
        InfamyService.recordStacks(mem, today, 10);
        InfamyService.recordStacks(mem, today, 10);
        assert InfamyService.tierActive(mem, today, InfamyTier.LEGEND);
        assert costFor(mem, today) == 25f : "LEGEND → 25";

        System.out.println("ALL PASSED");
    }

    private static float costFor(Map<String,Object> mem, int today) {
        return InfamyService.tierActive(mem, today, InfamyTier.LEGEND)
                ? UndergroundIntelAbility.COST_LEGEND : UndergroundIntelAbility.COST_BASE;
    }
}
