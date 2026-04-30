package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

/**
 * Awards Knowledge to AI-cored ships and records Combat Memory engagement after battles.
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

        // Award Knowledge to AI-cored ships
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet()
                .getFleetData().getMembersListCopy()) {
            if (member.getCaptain() == null || !member.getCaptain().isAICore()) continue;
            KnowledgeTracker.recordBattleWin(view, member.getCaptain().getId());
        }

        // Record Combat Memory engagement with primary enemy faction
        if (CollectiveActiveNodes.isActive("so_collective_combat_memory")) {
            CampaignFleetAPI loserFleet = result.getLoserResult() != null
                    ? result.getLoserResult().getFleet() : null;
            if (loserFleet != null && loserFleet.getFaction() != null) {
                CombatMemoryTracker.recordEngagement(view, loserFleet.getFaction().getId());
            }
        }
    }
}
