package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOEliteCascadeHandler implements EffectHandler {

    public static final String K_PURCHASED = "$skills.commandant.elite_cascade.purchased";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
    }
}
