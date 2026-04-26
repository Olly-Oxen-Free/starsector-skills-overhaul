package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.commandant.CommandantCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOCombatScriptHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CommandantCombatPlugin.ACTIVE_NODES.add(node);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CommandantCombatPlugin.ACTIVE_NODES.remove(node);
    }
}
