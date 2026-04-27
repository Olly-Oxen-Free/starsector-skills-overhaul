package skills_overhaul.effect;

import org.json.JSONObject;
import skills_overhaul.effect.handlers.PilotedManeuverabilityHandler;
import skills_overhaul.effect.handlers.PilotedWeaponDamageHandler;
import skills_overhaul.effect.handlers.corsair.SalvageMultiplierHandler;
import skills_overhaul.effect.handlers.corsair.StatBonusHandler;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {
    private static final Map<String, EffectHandler> handlers = new HashMap<>();

    static {
        handlers.put("piloted_maneuverability", new PilotedManeuverabilityHandler());
        handlers.put("piloted_weapon_damage",   new PilotedWeaponDamageHandler());
        handlers.put("corsair_stat_bonus",      new StatBonusHandler());
        handlers.put("corsair_salvage_mult",    new SalvageMultiplierHandler());
        handlers.put("corsair_combat_script",
                new skills_overhaul.effect.handlers.corsair.CombatScriptHandler());
        handlers.put("corsair_bounty_board",
                new skills_overhaul.effect.handlers.corsair.BountyBoardHandler());
        handlers.put("corsair_dmod_dp",
                new skills_overhaul.effect.handlers.corsair.DModDpScalingHandler());
        handlers.put("corsair_cap_mult",
                new skills_overhaul.effect.handlers.corsair.CapMultiplierHandler());
        handlers.put("corsair_t0",
                new skills_overhaul.effect.handlers.corsair.CorsairT0Handler());
        handlers.put("corsair_letter_of_marque",
                new skills_overhaul.effect.handlers.corsair.LetterOfMarqueHandler());
        handlers.put("SO_fleet_stat", new skills_overhaul.effect.handlers.SOFleetStatHandler());
        handlers.put("SO_combat_script", new skills_overhaul.effect.handlers.SOCombatScriptHandler());
        handlers.put("SO_commandant_t0", new skills_overhaul.effect.handlers.SOCommandantT0Handler());
        handlers.put("SO_command_field_mod", new skills_overhaul.effect.handlers.SOCommandFieldModHandler());
        handlers.put("SO_objective_scaling", new skills_overhaul.effect.handlers.SOObjectiveScalingHandler());
        handlers.put("SO_commandant_t5", new skills_overhaul.effect.handlers.SOCommandantT5Handler());
        handlers.put("SO_refitting", new skills_overhaul.effect.handlers.SORefittingHandler());
        handlers.put("SO_objective_capture_buff", new skills_overhaul.effect.handlers.SOObjectiveCaptureBuffHandler());
        handlers.put("SO_doctrine_hierarchy", new skills_overhaul.effect.handlers.SODoctrineHierarchyHandler());
        handlers.put("SO_officer_inspiration", new skills_overhaul.effect.handlers.SOOfficerInspirationHandler());
        handlers.put("SO_field_commands", new skills_overhaul.effect.handlers.SOFieldCommandsHandler());
        handlers.put("SO_elite_cascade", new skills_overhaul.effect.handlers.SOEliteCascadeHandler());
        handlers.put("SO_zeal_combat_script", new skills_overhaul.effect.handlers.SOZealCombatScriptHandler());
        handlers.put("SO_zealot_t0", new skills_overhaul.effect.handlers.SOZealotT0Handler());
        handlers.put("SO_zealot_t5", new skills_overhaul.effect.handlers.SOZealotT5Handler());
        handlers.put("SO_mobile_arsenal", new skills_overhaul.effect.handlers.SOMobileArsenalHandler());
        handlers.put("SO_field_logistics", new skills_overhaul.effect.handlers.SOFieldLogisticsHandler());
        handlers.put("SO_forced_march", new skills_overhaul.effect.handlers.SOForcedMarchHandler());
    }

    public static EffectHandler get(String type) {
        EffectHandler h = handlers.get(type);
        return h != null ? h : NoOpHandler.INSTANCE;
    }
}

class NoOpHandler implements EffectHandler {
    static final NoOpHandler INSTANCE = new NoOpHandler();
    @Override public void apply(String id, JSONObject p) {
        com.fs.starfarer.api.Global.getLogger(NoOpHandler.class)
                .warn("[SkillsOverhaul] unknown effect type on " + id + ": " + p.optString("type"));
    }
    @Override public void remove(String id, JSONObject p) {}
}
