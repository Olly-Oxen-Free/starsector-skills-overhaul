package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOHeavyDetachmentsHandler implements EffectHandler {

    public static final String STAT_KEY = "skills_overhaul_heavy_detachments";
    public static final String K_PURCHASED = "$skills.zealot.heavy_detachments.purchased";

    // Stats.GROUND_RAID_EFFECTIVENESS_MULT does not exist; nearest constant is "ground_attack_mod"
    private static final String GROUND_ATTACK_KEY = "ground_attack_mod";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        try {
            fleet.getStats().getDynamic().getMod(GROUND_ATTACK_KEY)
                    .modifyMult(STAT_KEY, 1.25f);
        } catch (Throwable t) {
            Global.getLogger(SOHeavyDetachmentsHandler.class).warn(
                    "[Zealot] HeavyDetachments raid effectiveness key issue: " + t.getMessage());
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            try {
                fleet.getStats().getDynamic().getMod(GROUND_ATTACK_KEY).unmodify(STAT_KEY);
            } catch (Throwable t) {}
        }
    }
}
