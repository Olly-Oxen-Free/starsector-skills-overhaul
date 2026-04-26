package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOObjectiveScalingHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        float fleetCapBonus = (float) params.optDouble("fleetCapBonus", 0.02);
        skills_overhaul.commandant.CommandFieldService.registerParam(
                nodeId, "fleetCapBonus", fleetCapBonus);
        Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.authority", true);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        skills_overhaul.commandant.CommandFieldService.unregisterParam(nodeId);
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.authority");
    }
}
