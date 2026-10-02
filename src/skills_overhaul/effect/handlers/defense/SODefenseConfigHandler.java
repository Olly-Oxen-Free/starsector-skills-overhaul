package skills_overhaul.effect.handlers.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.effect.PilotShipStatEffect;
import skills_overhaul.pilot.defense.DefenseCombatPlugin;

public final class SODefenseConfigHandler implements EffectHandler, PilotShipStatEffect {
    @Override
    public void apply(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.add(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        applyTo(ship, nodeId, params);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.remove(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        removeFrom(ship, nodeId, params);
    }

    @Override
    public void applyTo(ShipAPI ship, String nodeId, JSONObject params) {
        if (!ship.getVariant().hasHullMod("shield_shunt")) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getFluxDissipation().modifyMult(nodeId, 1.08f);
        s.getArmorBonus().modifyPercent(nodeId, 12f);
        s.getEmpDamageTakenMult().modifyMult(nodeId, 0.75f);
        s.getWeaponTurnRateBonus().modifyPercent(nodeId, 33f);
    }

    @Override
    public void removeFrom(ShipAPI ship, String nodeId, JSONObject params) {
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getFluxDissipation().unmodify(nodeId);
        s.getArmorBonus().unmodifyPercent(nodeId);
        s.getEmpDamageTakenMult().unmodify(nodeId);
        s.getWeaponTurnRateBonus().unmodifyPercent(nodeId);
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI e = Global.getCombatEngine();
        return e != null ? e.getPlayerShip() : null;
    }
}
