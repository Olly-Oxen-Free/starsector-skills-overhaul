package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.commandant.CommandFieldService;
import skills_overhaul.effect.EffectHandler;

public final class SOCommandFieldModHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String param = params.optString("param", null);
        if (param == null) return;
        float value = (float) params.optDouble("value", 0.0);
        CommandFieldService.registerParam(nodeId, param, value);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CommandFieldService.unregisterParam(nodeId);
    }
}
