package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Generic per-ship-stat handler for pilot tree nodes.
 * Reads named params from JSON and applies them to the player flagship's MutableShipStatsAPI.
 * Applied at node purchase (campaign layer); mods persist until respec.
 */
public final class SOPilotedStatHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        applyParams(s, nodeId, params, false);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        applyParams(s, nodeId, params, true);
    }

    private void applyParams(MutableShipStatsAPI s, String nodeId, JSONObject p, boolean remove) {
        if (p == null) return;
        java.util.Iterator<String> keys = p.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            float v = (float) p.optDouble(key, 0.0);
            if (remove) {
                unapply(s, nodeId, key);
            } else {
                applyKey(s, nodeId, key, v);
            }
        }
    }

    private void applyKey(MutableShipStatsAPI s, String id, String key, float v) {
        switch (key) {
            case "hullBonus":               s.getHullBonus().modifyPercent(id, v * 100f); break;
            case "armorBonus":              s.getArmorBonus().modifyPercent(id, v * 100f); break;
            case "fireChanceReduction":
                s.getEngineMalfunctionChance().modifyMult(id, 1f - v);
                s.getWeaponMalfunctionChance().modifyMult(id, 1f - v);
                break;
            case "fluxCapBonus":            s.getFluxCapacity().modifyMult(id, 1f + v); break;
            case "shieldEfficiency":        s.getShieldAbsorptionMult().modifyMult(id, 1f - v); break;
            case "shieldUpkeepReduction":   s.getShieldUpkeepMult().modifyMult(id, 1f - v); break;
            case "empDamageReduction":      s.getEmpDamageTakenMult().modifyMult(id, 1f - v); break;
            case "weaponRepairMult":        s.getCombatWeaponRepairTimeMult().modifyMult(id, 1f / v); break;
            case "engineRepairMult":        s.getCombatEngineRepairTimeMult().modifyMult(id, 1f / v); break;
            case "damageReduction":         s.getHullDamageTakenMult().modifyMult(id, 1f - v); break;
            case "pptBonus":                s.getPeakCRDuration().modifyFlat(id, v); break;
            case "projectileFragResist":    s.getFragmentationDamageTakenMult().modifyMult(id, 1f - v); break;
            case "fluxDissipationBonus":    s.getFluxDissipation().modifyMult(id, 1f + v); break;
            case "ballisticRofBonus":       s.getBallisticRoFMult().modifyMult(id, 1f + v); break;
            case "energyRofBonus":          s.getEnergyRoFMult().modifyMult(id, 1f + v); break;
            case "missileRofBonus":         s.getMissileRoFMult().modifyMult(id, 1f + v); break;
            case "weaponFluxCostReduction":
                s.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - v);
                s.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - v);
                break;
            case "projectileSpeedBonus":    s.getProjectileSpeedMult().modifyMult(id, 1f + v); break;
            case "weaponTurnRateBonus":     s.getWeaponTurnRateBonus().modifyPercent(id, v * 100f); break;
            case "nonMissileAmmoCapBonus":
                Global.getLogger(SOPilotedStatHandler.class)
                        .info("[SO] SOPilotedStatHandler: nonMissileAmmoCapBonus has no direct API, requires EveryFrame plugin logic");
                break;
            // Params with no direct API equivalents — log and skip
            case "weaponHPBonus": case "engineHPBonus": case "fighterBayHPBonus":
            case "moduleRepairMult": case "crDeclineStart": case "crRegenInCombat":
            case "massMult": case "collisionDamageDealt": case "crMalfunctionThreshold":
            case "hitChance": case "missileTrackingSpeed": case "ammoRegenBonus":
                Global.getLogger(SOPilotedStatHandler.class)
                        .info("[SO] SOPilotedStatHandler: param '" + key + "' handled in combat plugin, skipping static apply");
                break;
            default:
                Global.getLogger(SOPilotedStatHandler.class)
                        .warn("[SO] SOPilotedStatHandler: unknown param key '" + key + "' on " + id);
        }
    }

    private void unapply(MutableShipStatsAPI s, String id, String key) {
        switch (key) {
            case "hullBonus":               s.getHullBonus().unmodifyPercent(id); break;
            case "armorBonus":              s.getArmorBonus().unmodifyPercent(id); break;
            case "fireChanceReduction":
                s.getEngineMalfunctionChance().unmodify(id);
                s.getWeaponMalfunctionChance().unmodify(id);
                break;
            case "fluxCapBonus":            s.getFluxCapacity().unmodify(id); break;
            case "shieldEfficiency":        s.getShieldAbsorptionMult().unmodify(id); break;
            case "shieldUpkeepReduction":   s.getShieldUpkeepMult().unmodify(id); break;
            case "empDamageReduction":      s.getEmpDamageTakenMult().unmodify(id); break;
            case "weaponRepairMult":        s.getCombatWeaponRepairTimeMult().unmodify(id); break;
            case "engineRepairMult":        s.getCombatEngineRepairTimeMult().unmodify(id); break;
            case "damageReduction":         s.getHullDamageTakenMult().unmodify(id); break;
            case "pptBonus":                s.getPeakCRDuration().unmodifyFlat(id); break;
            case "projectileFragResist":    s.getFragmentationDamageTakenMult().unmodify(id); break;
            case "fluxDissipationBonus":    s.getFluxDissipation().unmodify(id); break;
            case "ballisticRofBonus":       s.getBallisticRoFMult().unmodify(id); break;
            case "energyRofBonus":          s.getEnergyRoFMult().unmodify(id); break;
            case "missileRofBonus":         s.getMissileRoFMult().unmodify(id); break;
            case "weaponFluxCostReduction":
                s.getBallisticWeaponFluxCostMod().unmodify(id);
                s.getEnergyWeaponFluxCostMod().unmodify(id);
                break;
            case "projectileSpeedBonus":    s.getProjectileSpeedMult().unmodify(id); break;
            case "weaponTurnRateBonus":     s.getWeaponTurnRateBonus().unmodifyPercent(id); break;
        }
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI engine = Global.getCombatEngine();
        return engine != null ? engine.getPlayerShip() : null;
    }
}
