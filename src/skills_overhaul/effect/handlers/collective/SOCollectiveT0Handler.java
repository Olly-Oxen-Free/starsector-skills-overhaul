package skills_overhaul.effect.handlers.collective;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.artificer.AutomatedShipCapApplier;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.collective.CollectiveAutomatedPoints;
import skills_overhaul.effect.EffectHandler;

public final class SOCollectiveT0Handler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        Global.getSector().getMemoryWithoutUpdate().set("$so_collective.active", true, 0f);
        // +60 automated ship points via the cap AutomatedShipCapApplier reads
        // ("so_automated_ship_points" is read by nothing).
        CollectiveAutomatedPoints.grantT0();
        AutomatedShipCapApplier.applyToPlayerFleet();
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        Global.getSector().getMemoryWithoutUpdate().unset("$so_collective.active");
        CollectiveAutomatedPoints.revokeT0();
        AutomatedShipCapApplier.applyToPlayerFleet();
    }
}
