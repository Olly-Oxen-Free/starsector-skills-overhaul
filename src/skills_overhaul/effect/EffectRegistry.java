package skills_overhaul.effect;

import org.json.JSONObject;
import skills_overhaul.effect.handlers.PilotedManeuverabilityHandler;
import skills_overhaul.effect.handlers.PilotedWeaponDamageHandler;
import skills_overhaul.effect.handlers.corsair.StatBonusHandler;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {
    private static final Map<String, EffectHandler> handlers = new HashMap<>();

    static {
        handlers.put("piloted_maneuverability", new PilotedManeuverabilityHandler());
        handlers.put("piloted_weapon_damage",   new PilotedWeaponDamageHandler());
        handlers.put("corsair_stat_bonus",      new StatBonusHandler());
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
