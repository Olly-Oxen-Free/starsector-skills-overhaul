package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

/**
 * Regression: respec previously filtered records by Tree.nodeIdPrefix()
 * (so_p_/so_c_/so_a_) which never matches real JSON node IDs. Players
 * lost all spent points on every respec. This test uses real-shape IDs.
 */
public class RespecRealNodeIdsTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<String,Object>();
        mem.put(SkillMemory.purchasedKey("so_defense_fortify"), true);
        mem.put(SkillMemory.purchasedKey("so_maneuver_drift_instinct"), true);
        SkillMemory.setPoints(mem, Tree.PILOT, 0);
        mem.put(SkillMemory.freeUniKey(), 0f);
        mem.put(SkillMemory.migUniKey(Tree.PILOT), 0f);

        RespecService.recordPurchase(mem, "so_defense_fortify", 1, 0, 0);
        RespecService.recordPurchase(mem, "so_maneuver_drift_instinct", 2, 1, 2);

        RespecService.Result r = RespecService.respecTree(mem, Tree.PILOT);
        assert r.storyPointsCharged == 0 : "first respec free";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 3
                : "TL returned even with real IDs (was: " + SkillMemory.getPoints(mem, Tree.PILOT) + ")";
        assert ((Number)mem.get(SkillMemory.freeUniKey())).floatValue() == 1f : "free uni returned";
        assert ((Number)mem.get(SkillMemory.migUniKey(Tree.PILOT))).floatValue() == 2f : "mig to tree";

        System.out.println("ALL PASSED");
    }
}
