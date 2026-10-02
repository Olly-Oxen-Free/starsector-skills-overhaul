package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Standing Orders (commandant_t1): CR recovery +3%/day for all ships, +4.5%/day for
 * officered ships (spec, commandant tree §6). Campaign-side fleet-member stat, same
 * pattern as SOMobileArsenalHandler; re-applied on load by NodeApplyService.
 */
public final class SOStandingOrdersHandler implements EffectHandler {

    public static final String STAT_KEY = "skills_overhaul_standing_orders";

    @Override
    public void apply(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            boolean officered = m.getCaptain() != null && !m.getCaptain().isDefault();
            m.getStats().getBaseCRRecoveryRatePercentPerDay().modifyFlat(STAT_KEY, officered ? 4.5f : 3f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.getStats().getBaseCRRecoveryRatePercentPerDay().unmodify(STAT_KEY);
        }
    }
}
