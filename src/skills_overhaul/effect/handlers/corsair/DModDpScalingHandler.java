package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Per-ship d-mod-scaled stat tweaks. Params:
 *   { "type": "SO_corsair_dmod_dp",
 *     "dpPerDMod":   -0.06,    // per-ship DP cost mult; not applicable — API lacks the stat
 *     "armorPerDMod": 0.03,    // armor bonus per d-mod
 *     "hullPerDMod":  0.03,    // hull bonus per d-mod
 *     "cap": 5 }
 *
 * V1 LIMITATION: dpPerDMod is documented but ignored. MutableShipStatsAPI on this
 * Starsector version exposes no per-ship DP-cost mutator (FleetMemberAPI.getDeploymentPointsCost()
 * is read-only). Armor/hull bonuses still apply via getArmorBonus()/getHullBonus().
 */
public final class DModDpScalingHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        int cap = params.optInt("cap", 5);
        float armor = (float) params.optDouble("armorPerDMod", 0.0);
        float hull  = (float) params.optDouble("hullPerDMod", 0.0);
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            int eff = Math.min(cap, countDMods(m));
            if (eff == 0) continue;
            if (armor != 0f) m.getStats().getArmorBonus().modifyMult(nodeId, 1f + armor * eff);
            if (hull != 0f)  m.getStats().getHullBonus().modifyMult(nodeId, 1f + hull * eff);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.getStats().getArmorBonus().unmodify(nodeId);
            m.getStats().getHullBonus().unmodify(nodeId);
        }
    }

    private int countDMods(FleetMemberAPI m) {
        // Vanilla "dmod" hullmod tag; the old id heuristic matched no vanilla d-mod id.
        return com.fs.starfarer.api.impl.campaign.DModManager.getNumDMods(m.getVariant());
    }
}
