package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.zealot.ZealotCombatPlugin;

public final class SOZealCombatScriptHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        ZealotCombatPlugin.ACTIVE_NODES.add(node);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        ZealotCombatPlugin.ACTIVE_NODES.remove(node);
    }
}
