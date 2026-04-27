package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.artificer.ArtificerCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOArtificerCombatScriptHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        ArtificerCombatPlugin.ACTIVE_NODES.add(node);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String node = params.optString("node", nodeId);
        ArtificerCombatPlugin.ACTIVE_NODES.remove(node);
    }
}
