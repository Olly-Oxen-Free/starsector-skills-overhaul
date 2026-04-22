package skills_overhaul.migration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import skills_overhaul.config.ConfigLoader;
import skills_overhaul.state.SkillMemory;

public final class MigrationPlugin {
    public static final String MIG_UNI_FREE_KEY = SkillMemory.freeUniKey();
    public static final String MIG_UNI_POOL_KEY = "$skills.points.universal_migrated_unallocated";

    public static void runOnce(boolean newGame) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (SkillMemory.isMigrated(mem)) return;

        int granted = 0;
        int spent = 0, unspent = 0;
        if (!newGame) {
            PersonAPI p = Global.getSector().getPlayerPerson();
            MutableCharacterStatsAPI s = p.getStats();
            spent   = countSpentNonElite(s);
            unspent = s.getPoints();
            granted = MigrationMath.grantUniversal(spent, unspent,
                    ConfigLoader.get().migrationMult, ConfigLoader.get().migrationCap);
            mem.set(MIG_UNI_POOL_KEY, (float) granted);
        }

        // Always grant 1 free universal at character creation (first load)
        mem.set(MIG_UNI_FREE_KEY, 1f);
        SkillMemory.setMigrated(mem, true);

        if (!newGame) {
            Global.getSector().getCampaignUI().addMessage(
                    "Skills Overhaul: migrated " + granted + " universal points (from " + spent + " spent + " + unspent + " unspent).");
        }
    }

    private static int countSpentNonElite(MutableCharacterStatsAPI s) {
        int n = 0;
        for (MutableCharacterStatsAPI.SkillLevelAPI sl : s.getSkillsCopy()) {
            if (sl.getLevel() < 1f) continue;
            n++;
        }
        return n;
    }
}
