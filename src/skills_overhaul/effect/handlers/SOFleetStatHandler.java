package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Generic fleet-stat handler. Reads { "stats": [ { "key": "...", "value": ... }, ... ] }.
 * Supported keys: sensorRangeMod, sensorProfileMod, fleetMaxBurnMod, fuelUseMod, suppliesUseMod.
 */
public final class SOFleetStatHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        JSONArray stats = params.optJSONArray("stats");
        if (stats == null) return;
        for (int i = 0; i < stats.length(); i++) {
            JSONObject s = stats.optJSONObject(i);
            if (s == null) continue;
            String key = s.optString("key", null);
            if (key == null) continue;
            float v = (float) s.optDouble("value", 0.0);
            apply(fleet.getStats(), nodeId, key, v);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        JSONArray stats = params.optJSONArray("stats");
        if (stats == null) return;
        for (int i = 0; i < stats.length(); i++) {
            JSONObject s = stats.optJSONObject(i);
            if (s == null) continue;
            String key = s.optString("key", null);
            if (key == null) continue;
            unapply(fleet.getStats(), nodeId, key);
        }
    }

    private void apply(MutableFleetStatsAPI s, String nodeId, String key, float v) {
        switch (key) {
            case "sensorRangeMod":     s.getSensorRangeMod().modifyMult(nodeId, 1f + v); break;
            case "sensorProfileMod":   s.getDetectedRangeMod().modifyMult(nodeId, 1f + v); break;
            case "fleetMaxBurnMod":    s.getFleetwideMaxBurnMod().modifyFlat(nodeId, v); break;
            case "fuelUseMod":         s.getFuelUseHyperMult().modifyMult(nodeId, 1f + v);
                                       s.getFuelUseNormalMult().modifyMult(nodeId, 1f + v); break;
            case "suppliesUseMod":
                Global.getLogger(SOFleetStatHandler.class).warn("[SO] suppliesUseMod not supported by MutableFleetStatsAPI — skipped"); break;
            default:
                Global.getLogger(SOFleetStatHandler.class).warn("[SO] unknown fleet stat key: " + key);
        }
    }

    private void unapply(MutableFleetStatsAPI s, String nodeId, String key) {
        switch (key) {
            case "sensorRangeMod":     s.getSensorRangeMod().unmodify(nodeId); break;
            case "sensorProfileMod":   s.getDetectedRangeMod().unmodify(nodeId); break;
            case "fleetMaxBurnMod":    s.getFleetwideMaxBurnMod().unmodify(nodeId); break;
            case "fuelUseMod":         s.getFuelUseHyperMult().unmodify(nodeId);
                                       s.getFuelUseNormalMult().unmodify(nodeId); break;
            case "suppliesUseMod":     break; // no API support
        }
    }
}
