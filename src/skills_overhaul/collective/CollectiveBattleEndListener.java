package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

/**
 * Awards Knowledge to AI-cored ships after battles where the Collective is active.
 * Registered as a transient listener in SkillsOverhaulModPlugin.onGameLoad().
 */
public final class CollectiveBattleEndListener extends BaseCampaignEventListener {

    public CollectiveBattleEndListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (!mem.getBoolean("$so_collective.active")) return;

        if (!result.didPlayerWin()) return;
        if (result.getWinnerResult() == null) return;
        if (result.getWinnerResult().getFleet() == null
                || !result.getWinnerResult().getFleet().isPlayerFleet()) return;

        MemoryView view = new MemoryAPIView(mem);
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet()
                .getFleetData().getMembersListCopy()) {
            if (member.getCaptain() == null || !member.getCaptain().isAICore()) continue;
            KnowledgeTracker.recordBattleWin(view, member.getCaptain().getId());
        }
    }
}
