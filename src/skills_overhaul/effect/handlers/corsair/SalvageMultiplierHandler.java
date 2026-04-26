package skills_overhaul.effect.handlers.corsair;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/** Params: { "type": "corsair_salvage_mult", "value": 0.20 } */
public final class SalvageMultiplierHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        float v = (float) params.optDouble("value", 0.0);
        PostBattleSalvageMultiplierListener.register(nodeId, 1f + v);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        PostBattleSalvageMultiplierListener.unregister(nodeId);
    }
}
