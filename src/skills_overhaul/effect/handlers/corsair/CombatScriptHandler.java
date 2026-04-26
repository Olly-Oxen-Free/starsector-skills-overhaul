package skills_overhaul.effect.handlers.corsair;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Effect type: "corsair_combat_script"
 * Params: { "type": "corsair_combat_script", "node": "<node id>" }
 *
 * Adds the node id to CorsairCombatPlugin.ACTIVE_NODES on apply, removes on remove.
 * CorsairCombatPlugin gates all per-frame effects on that set.
 */
public final class CombatScriptHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CorsairCombatPlugin.ACTIVE_NODES.add(node);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CorsairCombatPlugin.ACTIVE_NODES.remove(node);
    }
}
