package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.artificer.forge.FreeDeployQuotaTracker;
import skills_overhaul.effect.EffectHandler;

public final class SOForgeQuotaBonusHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String key = params.optString("key", nodeId);
        int value = params.optInt("value", 1);
        FreeDeployQuotaTracker.registerBonus(key, value);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String key = params.optString("key", nodeId);
        FreeDeployQuotaTracker.unregisterBonus(key);
    }
}
