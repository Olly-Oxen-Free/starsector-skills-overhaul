package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.json.JSONArray;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Generic fleet-stat handler. Reads { "stats": [ { "key": "...", "value": ... }, ... ] }.
 * Supported keys: sensorRangeMod, sensorProfileMod, fleetMaxBurnMod, fuelUseMod, suppliesUseMod,
 * planetaryOperationsMod (percent), planetaryOperationsCasualtiesMod (mult 1+v).
 * Per-member keys (applied to every current fleet member's stats, re-applied on load):
 * maxCombatReadinessFlat, officeredMaxCombatReadinessFlat, minCrewMod, crewLossMod (mult 1+v).
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
            if (applyMemberStat(fleet, nodeId, key, v, false)) continue;
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
            if (applyMemberStat(fleet, nodeId, key, 0f, true)) continue;
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
            case "planetaryOperationsMod":
                s.getDynamic().getMod(Stats.PLANETARY_OPERATIONS_MOD).modifyPercent(nodeId, v * 100f); break;
            case "planetaryOperationsCasualtiesMod":
                s.getDynamic().getStat(Stats.PLANETARY_OPERATIONS_CASUALTIES_MULT).modifyMult(nodeId, 1f + v); break;
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
            case "planetaryOperationsMod":
                s.getDynamic().getMod(Stats.PLANETARY_OPERATIONS_MOD).unmodify(nodeId); break;
            case "planetaryOperationsCasualtiesMod":
                s.getDynamic().getStat(Stats.PLANETARY_OPERATIONS_CASUALTIES_MULT).unmodify(nodeId); break;
            case "suppliesUseMod":     break; // no API support
        }
    }

    /** Per-member keys; returns false if {@code key} is not a member key. */
    private boolean applyMemberStat(CampaignFleetAPI fleet, String nodeId, String key, float v, boolean remove) {
        switch (key) {
            case "maxCombatReadinessFlat":
            case "officeredMaxCombatReadinessFlat":
            case "minCrewMod":
            case "crewLossMod":
                break;
            default:
                return false;
        }
        String id = nodeId + ":" + key;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            MutableShipStatsAPI st = m.getStats();
            switch (key) {
                case "maxCombatReadinessFlat":
                    if (remove) st.getMaxCombatReadiness().unmodify(id);
                    else st.getMaxCombatReadiness().modifyFlat(id, v);
                    break;
                case "officeredMaxCombatReadinessFlat":
                    boolean officered = m.getCaptain() != null && !m.getCaptain().isDefault();
                    if (remove || !officered) st.getMaxCombatReadiness().unmodify(id);
                    else st.getMaxCombatReadiness().modifyFlat(id, v);
                    break;
                case "minCrewMod":
                    if (remove) st.getMinCrewMod().unmodify(id);
                    else st.getMinCrewMod().modifyMult(id, 1f + v);
                    break;
                case "crewLossMod":
                    if (remove) st.getCrewLossMult().unmodify(id);
                    else st.getCrewLossMult().modifyMult(id, 1f + v);
                    break;
            }
        }
        return true;
    }
}
