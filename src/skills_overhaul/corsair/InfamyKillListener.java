package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class InfamyKillListener extends BaseCampaignEventListener {

    public InfamyKillListener() { super(false); }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (!result.didPlayerWin()) return;
        EngagementResultForFleetAPI loser = result.getLoserResult();
        if (loser == null) return;
        CampaignFleetAPI loserFleet = loser.getFleet();
        if (loserFleet == null) return;

        float fp = 0f;
        try {
            java.util.List<FleetMemberAPI> destroyed = loser.getDestroyed();
            if (destroyed != null) for (FleetMemberAPI m : destroyed) fp += m.getFleetPointCost();
            java.util.List<FleetMemberAPI> disabled = loser.getDisabled();
            if (disabled != null) for (FleetMemberAPI m : disabled) fp += m.getFleetPointCost();
        } catch (Throwable t) {
            fp = loserFleet.getFleetPoints();
        }

        if (fp <= 0f) return;
        int stacks = Math.min(10, (int) Math.ceil(fp / 30.0));
        int today = InfamyDailyTicker.monotonicDay();
        InfamyService.recordStacks(Global.getSector().getMemoryWithoutUpdate(), today, stacks);
    }
}
