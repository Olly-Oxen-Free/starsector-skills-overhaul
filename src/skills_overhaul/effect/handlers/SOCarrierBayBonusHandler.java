package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.artificer.ArtificerCombatPlugin;
import skills_overhaul.effect.EffectHandler;

public final class SOCarrierBayBonusHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        ArtificerCombatPlugin.ACTIVE_NODES.add("artificer_t4_carrier_command");
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.carrier_command.purchased", true);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        ArtificerCombatPlugin.ACTIVE_NODES.remove("artificer_t4_carrier_command");
        Global.getSector().getMemoryWithoutUpdate().unset(
                "$skills.artificer.carrier_command.purchased");
    }
}
