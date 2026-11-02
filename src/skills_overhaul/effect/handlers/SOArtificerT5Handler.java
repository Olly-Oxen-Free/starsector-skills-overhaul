package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOArtificerT5Handler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "core");
        if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.t5.purchased", true);
            skills_overhaul.effect.AbilityGrants.grant(skills_overhaul.artificer.alphastorm.AlphaStormAbility.ABILITY_ID);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "core");
        if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(
                    "$skills.artificer.t5.purchased");
            skills_overhaul.effect.AbilityGrants.revoke(skills_overhaul.artificer.alphastorm.AlphaStormAbility.ABILITY_ID);
        }
    }
}
