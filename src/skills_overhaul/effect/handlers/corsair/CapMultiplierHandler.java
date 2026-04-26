package skills_overhaul.effect.handlers.corsair;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.supply.SupplyBufferService;

/** Params: { "type": "corsair_cap_mult", "value": 1.5 } */
public final class CapMultiplierHandler implements EffectHandler {
    @Override public void apply(String nodeId, JSONObject params) {
        SupplyBufferService.registerCapMultiplier(nodeId, (float) params.optDouble("value", 1.0));
    }
    @Override public void remove(String nodeId, JSONObject params) {
        SupplyBufferService.unregisterCapMultiplier(nodeId);
    }
}
