package skills_overhaul.effect.handlers.defense;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.defense.DefenseCombatPlugin;

/** Registers a defense node as active so DefenseCombatPlugin can gate on it. */
public final class SODefenseCombatScriptHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.add(nodeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }
}
