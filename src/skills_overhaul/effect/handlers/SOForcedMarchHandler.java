package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOForcedMarchHandler implements EffectHandler {

    public static final String STAT_KEY = "skills_overhaul_forced_march";
    public static final String K_PURCHASED = "$skills.zealot.forced_march.purchased";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        fleet.getStats().getFleetwideMaxBurnMod().modifyFlat(STAT_KEY, 1f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            fleet.getStats().getFleetwideMaxBurnMod().unmodify(STAT_KEY);
        }
    }
}
