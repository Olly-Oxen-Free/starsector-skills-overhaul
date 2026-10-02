package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.zealot.ZealotCombatPlugin;

public final class SOZealotT5Handler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "core");
        if ("core".equals(channel)) {
            ZealotCombatPlugin.ACTIVE_NODES.add("zealot_t5");
            Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.t5.purchased", true);
            skills_overhaul.effect.AbilityGrants.grant(skills_overhaul.zealot.CrusadeAbility.ABILITY_ID);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "core");
        if ("core".equals(channel)) {
            ZealotCombatPlugin.ACTIVE_NODES.remove("zealot_t5");
            Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.t5.purchased");
            skills_overhaul.effect.AbilityGrants.revoke(skills_overhaul.zealot.CrusadeAbility.ABILITY_ID);
        }
    }
}
