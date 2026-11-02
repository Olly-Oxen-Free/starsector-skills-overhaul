package skills_overhaul.effect;

import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;

/**
 * Effect handlers whose effect is a static stat change on the player-piloted ship.
 *
 * Combat ships get fresh MutableShipStatsAPI per battle and getPlayerShip() is null
 * at purchase/load time, so these are (re)applied per combat by
 * SkillsOverhaulCombatPlugin, which also moves them when the player changes ship.
 */
public interface PilotShipStatEffect {
    void applyTo(ShipAPI ship, String nodeId, JSONObject params);
    void removeFrom(ShipAPI ship, String nodeId, JSONObject params);
}
