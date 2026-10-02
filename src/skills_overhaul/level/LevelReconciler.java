package skills_overhaul.level;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.EveryFrameScript;
import skills_overhaul.config.ConfigLoader;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

public class LevelReconciler implements EveryFrameScript {
    private float acc = 0f;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        acc += amount;
        if (acc < 1f) return;
        acc -= 1f;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        LevelService.Cfg cfg = new LevelService.Cfg(
                ConfigLoader.get().pointsPerLevel,
                ConfigLoader.get().bonusPointsEvery5);
        for (Tree t : Tree.values()) {
            long xp = (long) SkillMemory.getXP(mem, t);
            int currentLevel = SkillMemory.getLevel(mem, t);
            int targetLevel  = XPCurve.levelFromXP(xp);
            for (int lv = currentLevel + 1; lv <= targetLevel; lv++) {
                int granted = LevelService.pointsForLevel(lv, cfg);
                SkillMemory.addPoints(mem, t, granted);
                SkillMemory.setLevel(mem, t, lv);
                Global.getSector().getCampaignUI().addMessage(
                        "Skills Overhaul: " + t.key + " level " + lv + " (+" + granted + " points)");
            }
        }
    }
}
