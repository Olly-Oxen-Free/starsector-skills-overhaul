package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.artificer.ArtificerCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOFinalGambitHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        ArtificerCombatPlugin.ACTIVE_NODES.add("artificer_t3_final_gambit");
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.final_gambit.purchased", true);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        ArtificerCombatPlugin.ACTIVE_NODES.remove("artificer_t3_final_gambit");
        Global.getSector().getMemoryWithoutUpdate().unset(
                "$skills.artificer.final_gambit.purchased");
    }
}
