package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOStealthSystemsHandler implements EffectHandler {

    public static final String K_PURCHASED = "$skills.artificer.stealth.purchased";
    public static final String STAT_KEY = "skills_overhaul_artificer_stealth";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            fleet.getStats().getDetectedRangeMod().modifyMult(STAT_KEY, 0.75f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            fleet.getStats().getDetectedRangeMod().unmodify(STAT_KEY);
        }
    }
}
