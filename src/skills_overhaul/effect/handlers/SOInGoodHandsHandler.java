package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * In Good Hands (commandant_t3_in_good_hands), commandant tree spec §6:
 * per-ship DP cost reduced 0–10%, scaling with officer level (max −10% on a
 * level-6 officered ship). FleetMember-level, on Stats.DEPLOYMENT_POINTS_MOD.
 * Captain changes take effect on the next load/re-apply.
 */
public final class SOInGoodHandsHandler implements EffectHandler {

    public static final float MAX_REDUCTION = 0.10f;
    public static final float MAX_LEVEL = 6f;

    @Override
    public void apply(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            PersonAPI captain = m.getCaptain();
            float level = (captain == null || captain.isDefault()) ? 0f : captain.getStats().getLevel();
            float reduction = reductionForLevel(level);
            if (reduction <= 0f) {
                m.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(nodeId);
            } else {
                m.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(nodeId, 1f - reduction);
            }
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(nodeId);
        }
    }

    public static float reductionForLevel(float level) {
        if (level <= 0f) return 0f;
        return MAX_REDUCTION * Math.min(level, MAX_LEVEL) / MAX_LEVEL;
    }
}
