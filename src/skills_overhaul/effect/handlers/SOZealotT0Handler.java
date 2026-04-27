package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOZealotT0Handler implements EffectHandler {

    public static final String K_PURCHASED   = "$skills.zealot.t0.purchased";
    public static final String K_CORE_ACTIVE = "$skills.zealot.t0.coreActive";

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
            // Ballistic weapon range bonus +5% (applied in combat via ZealotCombatPlugin)
            Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.t0.ballistic_range_bonus", 0.05f);
            // Capital/Cruiser armor bonus +25 (applied in combat via ZealotCombatPlugin)
            Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.t0.cap_cruiser_armor_bonus", 25f);
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_CORE_ACTIVE, true);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.t0.ballistic_range_bonus");
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.t0.cap_cruiser_armor_bonus");
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_CORE_ACTIVE);
        }
    }
}
