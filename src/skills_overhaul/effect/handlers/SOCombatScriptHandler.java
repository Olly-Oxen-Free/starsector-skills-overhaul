package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.commandant.CommandantCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOCombatScriptHandler implements EffectHandler {

    private static final String T3_FLAG = "$so_commandant.tier_reached.3";

    @Override
    public void apply(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CommandantCombatPlugin.ACTIVE_NODES.add(node);
        if (node.contains("_t3_")) {
            Global.getSector().getMemoryWithoutUpdate().set(T3_FLAG, true, 0f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        CommandantCombatPlugin.ACTIVE_NODES.remove(node);
        if (node.contains("_t3_")) {
            boolean anyT3 = CommandantCombatPlugin.ACTIVE_NODES.stream()
                    .anyMatch(id -> id.contains("commandant_t3_"));
            if (!anyT3) Global.getSector().getMemoryWithoutUpdate().unset(T3_FLAG);
        }
    }
}
