package skills_overhaul.effect.handlers.collective;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.effect.EffectHandler;

public final class SOBattleConditioningHandler implements EffectHandler {

    private static final String SRC = "so_collective_battle_conditioning";

    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        // +20 automated ship points bonus
        Global.getSector().getPlayerPerson().getStats()
                .getDynamic().getStat("so_automated_ship_points")
                .modifyFlat(SRC, 20f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        Global.getSector().getPlayerPerson().getStats()
                .getDynamic().getStat("so_automated_ship_points")
                .unmodify(SRC);
    }
}
