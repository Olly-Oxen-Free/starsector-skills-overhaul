package skills_overhaul.effect.handlers.systems;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.systems.SystemsCombatPlugin;

/** Registers a systems node as active so SystemsCombatPlugin can gate on it. */
public final class SOSystemsCombatScriptHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.add(nodeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }
}
