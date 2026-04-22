package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.Map;

public final class LevelService {
    public static class Cfg {
        public final int perLevel;
        public final int bonusEvery5;
        public Cfg(int perLevel, int bonusEvery5) {
            this.perLevel = perLevel; this.bonusEvery5 = bonusEvery5;
        }
    }

    /** Grants points for a single level-up (newLevel is the level just reached). */
    public static int processLevelUp(Map<String,Object> mem, Tree tree, int newLevel, Cfg cfg) {
        int g = cfg.perLevel;
        if (newLevel > 0 && newLevel % 5 == 0) g += cfg.bonusEvery5;
        SkillMemory.addPoints(mem, tree, g);
        SkillMemory.setLevel(mem, tree, newLevel);
        return g;
    }

    /** Reconcile: pull XP → level; process any unprocessed level-ups. */
    public static void reconcileFromXP(Map<String,Object> mem, Tree tree, long xp, Cfg cfg) {
        int current = SkillMemory.getLevel(mem, tree);
        int target  = XPCurve.levelFromXP(xp);
        for (int lv = current + 1; lv <= target; lv++) processLevelUp(mem, tree, lv, cfg);
        SkillMemory.setLevel(mem, tree, target);
    }
}
