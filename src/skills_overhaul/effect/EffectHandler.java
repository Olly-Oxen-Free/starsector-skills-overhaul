package skills_overhaul.effect;

import org.json.JSONObject;

/**
 * Purchase-time effect wiring. Each handler chooses its own stat target:
 *   - campaign handlers grab Global.getSector().getPlayerPerson().getStats()
 *   - combat/piloted handlers register a CombatPlugin and mutate MutableShipStatsAPI at combat start
 * Keeping stats out of the signature avoids locking the interface to character-level stats.
 */
public interface EffectHandler {
    void apply(String nodeId, JSONObject params);
    void remove(String nodeId, JSONObject params);
}
