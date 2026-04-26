package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public final class BountyBoardListener extends BaseCampaignEventListener {

    public static volatile boolean active = false;
    public static volatile float weaponDropMult = 1f;

    public BountyBoardListener() { super(false); }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (!active || !result.didPlayerWin()) return;
        EngagementResultForFleetAPI loser = result.getLoserResult();
        if (loser == null) return;
        int kills = 0;
        List<FleetMemberAPI> destroyed = loser.getDestroyed();
        if (destroyed != null) kills += destroyed.size();
        List<FleetMemberAPI> disabled = loser.getDisabled();
        if (disabled != null) kills += disabled.size();
        if (kills > 0) {
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(kills * 800f);
        }
    }
}
