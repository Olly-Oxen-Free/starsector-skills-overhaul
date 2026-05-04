package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public class PilotedWeaponDamageHandler implements EffectHandler {

    private static final String MEM_PREFIX = "$so_piloted_weapon_damage.";

    @Override
    public void apply(String nodeId, JSONObject p) {
        float amt = (float) p.optDouble("amount", 0);
        String filter = p.optString("filter", "ALL");
        float mult = 1f + amt;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null) {
                applyToStats(player.getMutableStats(), nodeId, filter, mult);
            }
        } else {
            Global.getSector().getMemoryWithoutUpdate().set(
                    MEM_PREFIX + nodeId + ".mult", mult, 0f);
            Global.getSector().getMemoryWithoutUpdate().set(
                    MEM_PREFIX + nodeId + ".filter", filter, 0f);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject p) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null) {
                MutableShipStatsAPI stats = player.getMutableStats();
                stats.getBallisticWeaponDamageMult().unmodify(nodeId);
                stats.getEnergyWeaponDamageMult().unmodify(nodeId);
                stats.getMissileWeaponDamageMult().unmodify(nodeId);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().unset(MEM_PREFIX + nodeId + ".mult");
        Global.getSector().getMemoryWithoutUpdate().unset(MEM_PREFIX + nodeId + ".filter");
    }

    static void applyToStats(MutableShipStatsAPI stats, String key, String filter, float mult) {
        switch (filter) {
            case "BALLISTIC":
                stats.getBallisticWeaponDamageMult().modifyMult(key, mult);
                break;
            case "ENERGY":
                stats.getEnergyWeaponDamageMult().modifyMult(key, mult);
                break;
            case "MISSILE":
                stats.getMissileWeaponDamageMult().modifyMult(key, mult);
                break;
            default:
                stats.getBallisticWeaponDamageMult().modifyMult(key, mult);
                stats.getEnergyWeaponDamageMult().modifyMult(key, mult);
                stats.getMissileWeaponDamageMult().modifyMult(key, mult);
                break;
        }
    }
}
