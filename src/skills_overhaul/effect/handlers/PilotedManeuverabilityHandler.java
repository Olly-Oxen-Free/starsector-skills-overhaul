package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public class PilotedManeuverabilityHandler implements EffectHandler {

    private static final String MEM_PREFIX = "$so_piloted_maneuverability.";

    @Override
    public void apply(String nodeId, JSONObject p) {
        float amt = (float) p.optDouble("amount", 0);
        float pct = amt * 100f;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null) {
                player.getMutableStats().getTurnAcceleration().modifyPercent(nodeId, pct);
                player.getMutableStats().getMaxTurnRate().modifyPercent(nodeId, pct);
            }
        } else {
            Global.getSector().getMemoryWithoutUpdate().set(MEM_PREFIX + nodeId, pct, 0f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject p) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null) {
                player.getMutableStats().getTurnAcceleration().unmodify(nodeId);
                player.getMutableStats().getMaxTurnRate().unmodify(nodeId);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().unset(MEM_PREFIX + nodeId);
    }
}
