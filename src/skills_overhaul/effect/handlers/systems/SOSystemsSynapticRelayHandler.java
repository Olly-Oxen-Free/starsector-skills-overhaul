package skills_overhaul.effect.handlers.systems;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.systems.SynapticRelayService;
import skills_overhaul.pilot.systems.SystemsCombatPlugin;

public final class SOSystemsSynapticRelayHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.add(nodeId);
        SynapticRelayService.applyToFleet();
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }
}
