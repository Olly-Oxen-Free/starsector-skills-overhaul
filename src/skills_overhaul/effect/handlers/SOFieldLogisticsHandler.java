package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOFieldLogisticsHandler implements EffectHandler {

    public static final String K_PURCHASED = "$skills.zealot.field_logistics.purchased";
    public static final String STAT_KEY = "skills_overhaul_field_logistics";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.getStats().getSuppliesPerMonth().modifyMult(STAT_KEY, 0.90f);
            m.getStats().getFuelUseMod().modifyMult(STAT_KEY, 0.95f);
            m.getStats().getCRPerDeploymentPercent().modifyMult(STAT_KEY, 1.20f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                m.getStats().getSuppliesPerMonth().unmodify(STAT_KEY);
                m.getStats().getFuelUseMod().unmodify(STAT_KEY);
                m.getStats().getCRPerDeploymentPercent().unmodify(STAT_KEY);
            }
        }
    }
}
