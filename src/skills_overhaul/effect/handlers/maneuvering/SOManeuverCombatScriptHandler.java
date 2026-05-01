package skills_overhaul.effect.handlers.maneuvering;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.maneuvering.ManeuveringCombatPlugin;

public final class SOManeuverCombatScriptHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        ManeuveringCombatPlugin.ACTIVE_NODES.add(nodeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        ManeuveringCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }
}
