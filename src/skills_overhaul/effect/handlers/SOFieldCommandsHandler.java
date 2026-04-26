package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.commandant.FieldCommandsService;
import skills_overhaul.effect.EffectHandler;

public final class SOFieldCommandsHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        FieldCommandsService.active = true;
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        FieldCommandsService.active = false;
    }
}
