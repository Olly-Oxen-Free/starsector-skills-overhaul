package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

// FIXME(Phase 10.4): real weapon-damage bonus must apply to piloted ship's MutableShipStatsAPI
// via combat plugin. Stub logs intent; wire actual stat change when CombatPluginSkillEffects lands.
public class PilotedWeaponDamageHandler implements EffectHandler {
    @Override public void apply(String nodeId, JSONObject p) {
        float amt = (float) p.optDouble("amount", 0);
        String filter = p.optString("filter", "ALL");
        Global.getLogger(PilotedWeaponDamageHandler.class)
                .info("[SkillsOverhaul] STUB apply piloted_weapon_damage " + nodeId + " amount=" + amt + " filter=" + filter);
    }
    @Override public void remove(String nodeId, JSONObject p) {
        Global.getLogger(PilotedWeaponDamageHandler.class)
                .info("[SkillsOverhaul] STUB remove piloted_weapon_damage " + nodeId);
    }
}
