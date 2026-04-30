package skills_overhaul.effect.handlers.collective;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.effect.EffectHandler;

public final class SOCollectiveT0Handler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        Global.getSector().getMemoryWithoutUpdate().set("$so_collective.active", true, 0f);
        Global.getSector().getPlayerPerson().getStats()
                .getDynamic().getStat("so_automated_ship_points")
                .modifyFlat("so_collective_t0", 60f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        Global.getSector().getMemoryWithoutUpdate().unset("$so_collective.active");
        Global.getSector().getPlayerPerson().getStats()
                .getDynamic().getStat("so_automated_ship_points")
                .unmodify("so_collective_t0");
    }
}
