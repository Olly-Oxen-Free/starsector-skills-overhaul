package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

/** Review 2026-11-02-0039-ui B3: a paid respec must be refused without story points. */
public class RespecAffordTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<String,Object>();

        // First respec of a tree is free, even at 0 SP.
        assert RespecService.canAfford(mem, Tree.PILOT, 0) : "first respec free at 0 SP";

        mem.put(SkillMemory.respecUsedKey(Tree.PILOT), true);
        assert !RespecService.canAfford(mem, Tree.PILOT, 0) : "paid respec refused at 0 SP";
        assert RespecService.canAfford(mem, Tree.PILOT, 1) : "paid respec allowed at 1 SP";

        // Free respec is tracked per tree.
        assert RespecService.canAfford(mem, Tree.COMMANDER, 0) : "other tree still free";

        // canAfford agrees with what respecTree charges.
        mem.put(SkillMemory.purchasedKey("n"), true);
        RespecService.recordPurchase(mem, "n", 1, 0, 0);
        RespecService.Result r = RespecService.respecTree(mem, Tree.PILOT);
        assert r.storyPointsCharged == 1;

        System.out.println("ALL PASSED");
    }
}
