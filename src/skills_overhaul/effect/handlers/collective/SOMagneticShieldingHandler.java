package skills_overhaul.effect.handlers.collective;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.effect.EffectHandler;

public final class SOMagneticShieldingHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        Global.getSector().getMemoryWithoutUpdate().set("$so_collective.terrain_immune", true, 0f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        Global.getSector().getMemoryWithoutUpdate().unset("$so_collective.terrain_immune");
    }
}
