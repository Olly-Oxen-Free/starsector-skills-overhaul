package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.artificer.AutomatedShipPointService;
import skills_overhaul.effect.EffectHandler;

public final class SOAutomatedPointsBonusHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String key = params.optString("key", nodeId);
        int value = params.optInt("value", 40);
        AutomatedShipPointService.registerBonus(key, value);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String key = params.optString("key", nodeId);
        AutomatedShipPointService.unregisterBonus(key);
    }
}
