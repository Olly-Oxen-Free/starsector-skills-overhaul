package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.commandant.CommandantCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOCommandantT5Handler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "core");
        if ("core".equals(channel)) {
            CommandantCombatPlugin.ACTIVE_NODES.add("commandant_t5");
            Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.t5.purchased", true);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "core");
        if ("core".equals(channel)) {
            CommandantCombatPlugin.ACTIVE_NODES.remove("commandant_t5");
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.t5.purchased");
        }
    }
}
