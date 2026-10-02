package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Effect type: "SO_corsair_combat_script"
 * Params: { "type": "SO_corsair_combat_script", "node": "<node id>" }
 *
 * Adds the node id to CorsairCombatPlugin.ACTIVE_NODES on apply, removes on remove.
 * CorsairCombatPlugin gates all per-frame effects on that set.
 */
public final class CombatScriptHandler implements EffectHandler {

    private static final String T3_FLAG = "$so_corsair.tier_reached.3";

    @Override
    public void apply(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CorsairCombatPlugin.ACTIVE_NODES.add(node);
        if (node.contains("_t3_")) {
            Global.getSector().getMemoryWithoutUpdate().set(T3_FLAG, true, 0f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CorsairCombatPlugin.ACTIVE_NODES.remove(node);
        if (node.contains("_t3_")) {
            boolean anyT3 = CorsairCombatPlugin.ACTIVE_NODES.stream()
                    .anyMatch(id -> id.contains("corsair_t3_"));
            if (!anyT3) Global.getSector().getMemoryWithoutUpdate().unset(T3_FLAG);
        }
    }
}
