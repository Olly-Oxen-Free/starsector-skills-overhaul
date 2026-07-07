package skills_overhaul.xp;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import skills_overhaul.api.SkillsOverhaulAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

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

    /**
     * Resolve the sub-mod (crew XP) commander multiplier, defaulting to 1.0 for a
     * missing/degenerate provider. Package-visible + pure so it can be unit-tested
     * without a live sector.
     */
    static float commanderMultiplier() {
        float mult;
        try {
            mult = SkillsOverhaulAPI.getCommanderNumericMultiplier();
        } catch (Throwable t) {
            mult = 1.0f;
        }
        if (mult <= 0f || Float.isNaN(mult)) mult = 1.0f;
        return mult;
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
            // Layer the sub-mod integration multiplier onto the mirrored delta.
            float awarded = (float) delta * commanderMultiplier();
            SkillMemory.addXP(Global.getSector().getMemoryWithoutUpdate(), Tree.COMMANDER, awarded);
        }
        lastSeenVanillaXP = nowXP;
    }
}
