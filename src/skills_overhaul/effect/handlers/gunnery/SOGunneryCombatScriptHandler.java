package skills_overhaul.effect.handlers.gunnery;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.gunnery.GunneryCombatPlugin;

/** Registers a gunnery node as active so GunneryCombatPlugin can gate on it. */
public final class SOGunneryCombatScriptHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        GunneryCombatPlugin.ACTIVE_NODES.add(nodeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        GunneryCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }
}
