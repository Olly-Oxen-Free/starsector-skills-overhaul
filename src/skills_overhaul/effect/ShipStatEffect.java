package skills_overhaul.effect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;

/**
 * Per-ship stat part of an effect. MutableShipStatsAPI is rebuilt per combat, so
 * SkillsOverhaulCombatPlugin calls this on the piloted ship at the start of each battle;
 * purchase and load run in the campaign, where there is no ship to modify.
 */
public interface ShipStatEffect {
    void applyTo(MutableShipStatsAPI s, ShipAPI ship, String nodeId, JSONObject params);
}
