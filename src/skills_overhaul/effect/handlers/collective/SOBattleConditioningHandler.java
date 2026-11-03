package skills_overhaul.effect.handlers.collective;

import org.json.JSONObject;
import skills_overhaul.artificer.AutomatedShipCapApplier;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.collective.CollectiveAutomatedPoints;
import skills_overhaul.effect.EffectHandler;

public final class SOBattleConditioningHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        // +20 automated ship points via the cap AutomatedShipCapApplier reads
        CollectiveAutomatedPoints.grantBattleConditioning();
        AutomatedShipCapApplier.applyToPlayerFleet();
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        CollectiveAutomatedPoints.revokeBattleConditioning();
        AutomatedShipCapApplier.applyToPlayerFleet();
    }
}
