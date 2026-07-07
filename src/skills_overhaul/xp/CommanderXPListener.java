package skills_overhaul.xp;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.Map;

public class CommanderXPListener extends BaseCampaignEventListener {
    public CommanderXPListener() {
        super(false);
        // Seed the baseline from the player's current vanilla XP at registration
        // time (constructor runs during onGameLoad, before any post-load
        // engagement). This prevents the first engagement after a game load from
        // only establishing a baseline and silently dropping its XP delta.
        if (Global.getSector() != null && Global.getSector().getPlayerStats() != null) {
            lastSeenVanillaXP = Global.getSector().getPlayerStats().getXP();
        }
    }

    public static void mirror(Map<String,Object> mem, float awarded) {
        SkillMemory.addXP(mem, Tree.COMMANDER, awarded);
    }

    private long lastSeenVanillaXP = -1L;

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        long nowXP = Global.getSector().getPlayerStats().getXP();
        // Fallback seeding in case the constructor ran before the sector/player
        // stats were available; without it the first delta would be dropped.
        if (lastSeenVanillaXP < 0L) { lastSeenVanillaXP = nowXP; return; }
        long delta = nowXP - lastSeenVanillaXP;
        if (delta > 0L) {
            SkillMemory.addXP(Global.getSector().getMemoryWithoutUpdate(), Tree.COMMANDER, (float) delta);
        }
        lastSeenVanillaXP = nowXP;
    }
}
