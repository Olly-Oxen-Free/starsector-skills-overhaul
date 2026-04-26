package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SORefittingHandler implements EffectHandler {
    public static final String K_PURCHASED = "$skills.commandant.refitting.purchased";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.refitting.recovery_chance_bonus", 0.40f);
        Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.refitting.dmod_avoid", 0.75f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.refitting.recovery_chance_bonus");
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.refitting.dmod_avoid");
    }
}
