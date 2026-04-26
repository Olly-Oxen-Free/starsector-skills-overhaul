package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.commandant.CommandantCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOOfficerInspirationHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CommandantCombatPlugin.ACTIVE_NODES.add(nodeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CommandantCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }
}
