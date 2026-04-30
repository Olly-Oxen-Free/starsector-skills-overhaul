package skills_overhaul.effect.handlers.collective;

import org.json.JSONObject;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.effect.EffectHandler;

public final class SOVoltaicConduitsHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
    }
}
