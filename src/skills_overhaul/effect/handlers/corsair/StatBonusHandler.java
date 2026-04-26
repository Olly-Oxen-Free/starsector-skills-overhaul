package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Generic stat-bonus handler. Reads a list of stat tweaks from JSON params and applies them
 * to the player fleet (scope=fleet) or each ship in it (scope=ship).
 *
 * Params shape:
 *   { "type": "corsair_stat_bonus",
 *     "scope": "fleet" | "ship",
 *     "stats": [ { "key": "sensorProfileMod", "value": -0.10 }, ... ] }
 *
 * Supported keys (extend as nodes need new ones):
 *   fleet:
 *     sensorProfileMod         — multiplies fleet detected-range  (modifyMult 1+v)
 *     fleetManeuverabilityMod  — flat add to fleet max burn        (modifyFlat v)
 *   ship:
 *     dpModifier               — unsupported via MutableShipStatsAPI; logs warning
 *     crModifier               — flat add to max CR                (modifyFlat v)
 */
public final class StatBonusHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        JSONArray stats = params.optJSONArray("stats");
        if (stats == null) return;
        String scope = params.optString("scope", "fleet");
        for (int i = 0; i < stats.length(); i++) {
            JSONObject s = stats.optJSONObject(i);
            if (s == null) continue;
            String key = s.optString("key", null);
            if (key == null) continue;
            float value = (float) s.optDouble("value", 0.0);
            if ("fleet".equals(scope)) {
                applyFleetStat(fleet.getStats(), nodeId, key, value);
            } else {
                for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                    applyShipStat(m.getStats(), nodeId, key, value);
                }
            }
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        JSONArray stats = params.optJSONArray("stats");
        if (stats == null) return;
        String scope = params.optString("scope", "fleet");
        for (int i = 0; i < stats.length(); i++) {
            JSONObject s = stats.optJSONObject(i);
            if (s == null) continue;
            String key = s.optString("key", null);
            if (key == null) continue;
            if ("fleet".equals(scope)) {
                unapplyFleetStat(fleet.getStats(), nodeId, key);
            } else {
                for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                    unapplyShipStat(m.getStats(), nodeId, key);
                }
            }
        }
    }

    private void applyFleetStat(MutableFleetStatsAPI s, String nodeId, String key, float value) {
        switch (key) {
            case "sensorProfileMod":
                s.getDetectedRangeMod().modifyMult(nodeId, 1f + value);
                break;
            case "fleetManeuverabilityMod":
                s.getFleetwideMaxBurnMod().modifyFlat(nodeId, value);
                break;
            default:
                Global.getLogger(StatBonusHandler.class).warn(
                        "[SkillsOverhaul] unknown fleet stat key: " + key);
        }
    }

    private void unapplyFleetStat(MutableFleetStatsAPI s, String nodeId, String key) {
        switch (key) {
            case "sensorProfileMod":        s.getDetectedRangeMod().unmodify(nodeId); break;
            case "fleetManeuverabilityMod": s.getFleetwideMaxBurnMod().unmodify(nodeId); break;
        }
    }

    private void applyShipStat(MutableShipStatsAPI s, String nodeId, String key, float value) {
        switch (key) {
            case "crModifier":
                s.getMaxCombatReadiness().modifyFlat(nodeId, value);
                break;
            case "dpModifier":
                Global.getLogger(StatBonusHandler.class).warn(
                        "[SkillsOverhaul] dpModifier not supported via MutableShipStatsAPI on " + nodeId
                        + "; use a FleetMemberAPI-level hook instead");
                break;
            default:
                Global.getLogger(StatBonusHandler.class).warn(
                        "[SkillsOverhaul] unknown ship stat key: " + key);
        }
    }

    private void unapplyShipStat(MutableShipStatsAPI s, String nodeId, String key) {
        switch (key) {
            case "crModifier": s.getMaxCombatReadiness().unmodify(nodeId); break;
        }
    }
}
