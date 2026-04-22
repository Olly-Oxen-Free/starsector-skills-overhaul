package skills_overhaul.xp;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.Map;

public class CommanderXPListener extends BaseCampaignEventListener {
    public CommanderXPListener() { super(false); }

    public static void mirror(Map<String,Object> mem, float awarded) {
        SkillMemory.addXP(mem, Tree.COMMANDER, awarded);
    }

    private long lastSeenVanillaXP = -1L;

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        long nowXP = Global.getSector().getPlayerStats().getXP();
        if (lastSeenVanillaXP < 0L) { lastSeenVanillaXP = nowXP; return; }
        long delta = nowXP - lastSeenVanillaXP;
        if (delta > 0L) {
            SkillMemory.addXP(Global.getSector().getMemoryWithoutUpdate(), Tree.COMMANDER, (float) delta);
        }
        lastSeenVanillaXP = nowXP;
    }
}
