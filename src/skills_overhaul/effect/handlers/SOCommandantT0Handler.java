package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOCommandantT0Handler implements EffectHandler {

    public static final String K_PURCHASED   = "$skills.commandant.t0.purchased";
    public static final String K_CORE_ACTIVE = "$skills.commandant.t0.coreActive";

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
            Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.officer_market_freq", 1.5f);
            Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.officer_xp_mult", 1.20f);
            Global.getSector().getMemoryWithoutUpdate().set("$skills.commandant.officer_salary_mult", 0.80f);
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_CORE_ACTIVE, true);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.officer_market_freq");
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.officer_xp_mult");
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.commandant.officer_salary_mult");
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_CORE_ACTIVE);
        }
    }
}
