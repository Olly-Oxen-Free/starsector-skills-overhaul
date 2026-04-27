package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SONavMatrixHandler implements EffectHandler {

    public static final String STAT_KEY = "skills_overhaul_nav_matrix";

    @Override
    public void apply(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        // +1 burn for ships <9 burn
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            float baseBurn;
            try { baseBurn = m.getStats().getMaxBurnLevel().getBaseValue(); }
            catch (Throwable t) { baseBurn = 8f; }
            if (baseBurn < 9f) {
                m.getStats().getMaxBurnLevel().modifyFlat(STAT_KEY, 1f);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().set("$skills.artificer.nav_matrix.purchased", true);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                m.getStats().getMaxBurnLevel().unmodify(STAT_KEY);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.artificer.nav_matrix.purchased");
    }
}
