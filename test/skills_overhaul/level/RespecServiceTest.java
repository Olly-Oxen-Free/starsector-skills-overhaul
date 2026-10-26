package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

public class RespecServiceTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<String,Object>();
        mem.put(SkillMemory.purchasedKey("so_p_g0"), true);
        mem.put(SkillMemory.purchasedKey("so_p_m0"), true);
        SkillMemory.setPoints(mem, Tree.PILOT, 0);
        mem.put(SkillMemory.freeUniKey(), 0f);
        mem.put(SkillMemory.migUniKey(Tree.PILOT), 0f);

        RespecService.recordPurchase(mem, "so_p_g0", 1, 0, 0);
        RespecService.recordPurchase(mem, "so_p_m0", 2, 1, 2);

        RespecService.Result r = RespecService.respecTree(mem, Tree.PILOT);
        assert r.storyPointsCharged == 0 : "first respec free";
        assert SkillMemory.getPoints(mem, Tree.PILOT) == 3 : "TL returned";
        assert ((Number)mem.get(SkillMemory.freeUniKey())).floatValue() == 1f : "free uni";
        assert ((Number)mem.get(SkillMemory.migUniKey(Tree.PILOT))).floatValue() == 2f : "mig to tree";
        assert !Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey("so_p_g0"))) : "node cleared";

        mem.put(SkillMemory.purchasedKey("so_p_g0"), true);
        RespecService.recordPurchase(mem, "so_p_g0", 1, 0, 0);
        r = RespecService.respecTree(mem, Tree.PILOT);
        assert r.storyPointsCharged == 1 : "second respec 1 SP";

        Map<String,Object> snap = new HashMap<String,Object>();
        assert RespecService.costFor(snap, Tree.PILOT) == 0 : "unset -> free";
        snap.put(SkillMemory.respecUsedKey(Tree.PILOT), false);
        assert RespecService.costFor(snap, Tree.PILOT) == 0 : "false -> free";
        snap.put(SkillMemory.respecUsedKey(Tree.PILOT), true);
        assert RespecService.costFor(snap, Tree.PILOT) == 1 : "used -> 1 SP";
        assert RespecService.costFor(snap, Tree.COMMANDER) == 0 : "other tree unaffected";

        System.out.println("ALL PASSED");
    }
}
