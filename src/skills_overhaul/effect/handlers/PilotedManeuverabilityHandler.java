package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

// FIXME(Phase 10.4): real turn-rate bonus must apply to piloted ship's MutableShipStatsAPI
// via a combat plugin. Stub logs intent; wire actual stat change when CombatPluginSkillEffects lands.
public class PilotedManeuverabilityHandler implements EffectHandler {
    @Override public void apply(String nodeId, JSONObject p) {
        float amt = (float) p.optDouble("amount", 0);
        Global.getLogger(PilotedManeuverabilityHandler.class)
                .info("[SkillsOverhaul] STUB apply piloted_maneuverability " + nodeId + " amount=" + amt);
    }
    @Override public void remove(String nodeId, JSONObject p) {
        Global.getLogger(PilotedManeuverabilityHandler.class)
                .info("[SkillsOverhaul] STUB remove piloted_maneuverability " + nodeId);
    }
}
