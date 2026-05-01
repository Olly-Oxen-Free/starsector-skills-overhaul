package skills_overhaul.effect.handlers.systems;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.systems.SystemsCombatPlugin;

public final class SOSystemsAutomataIntegraHandler implements EffectHandler {
    private static final String MEM_KEY = "$so_automata_integra_enabled";

    @Override
    public void apply(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.add(nodeId);
        Global.getSector().getMemoryWithoutUpdate().set(MEM_KEY, true);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.remove(nodeId);
        Global.getSector().getMemoryWithoutUpdate().unset(MEM_KEY);
    }
}
