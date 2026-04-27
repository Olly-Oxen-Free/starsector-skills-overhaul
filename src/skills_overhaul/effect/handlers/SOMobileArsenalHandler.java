package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOMobileArsenalHandler implements EffectHandler {

    public static final String K_PURCHASED = "$skills.zealot.mobile_arsenal.purchased";
    public static final String STAT_KEY = "skills_overhaul_mobile_arsenal";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.getStats().getCargoMod().modifyMult(STAT_KEY, 1.20f);
            m.getStats().getSuppliesPerMonth().modifyMult(STAT_KEY, 0.80f);
        }
        Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.mobile_arsenal.post_battle_cr", 0.10f);
        Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.mobile_arsenal.post_battle_hull", 0.10f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                m.getStats().getCargoMod().unmodify(STAT_KEY);
                m.getStats().getSuppliesPerMonth().unmodify(STAT_KEY);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.mobile_arsenal.post_battle_cr");
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.mobile_arsenal.post_battle_hull");
    }
}
