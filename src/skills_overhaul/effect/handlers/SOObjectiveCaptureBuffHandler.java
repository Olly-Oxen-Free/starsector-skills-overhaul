package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.commandant.CommandantCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOObjectiveCaptureBuffHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CommandantCombatPlugin.ACTIVE_NODES.add(nodeId);
        Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.war_room.officer_slot_bonus", 2);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CommandantCombatPlugin.ACTIVE_NODES.remove(nodeId);
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.war_room.officer_slot_bonus");
    }
}
