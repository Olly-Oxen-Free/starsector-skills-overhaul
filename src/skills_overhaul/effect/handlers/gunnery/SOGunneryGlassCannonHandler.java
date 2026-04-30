package skills_overhaul.effect.handlers.gunnery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.gunnery.GunneryCombatPlugin;

public final class SOGunneryGlassCannonHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        GunneryCombatPlugin.ACTIVE_NODES.add(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getBallisticWeaponDamageMult().modifyMult(nodeId, 1.30f);
        s.getEnergyWeaponDamageMult().modifyMult(nodeId, 1.30f);
        s.getMissileWeaponDamageMult().modifyMult(nodeId, 1.30f);
        s.getHullDamageTakenMult().modifyMult(nodeId, 1.20f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        GunneryCombatPlugin.ACTIVE_NODES.remove(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getBallisticWeaponDamageMult().unmodify(nodeId);
        s.getEnergyWeaponDamageMult().unmodify(nodeId);
        s.getMissileWeaponDamageMult().unmodify(nodeId);
        s.getHullDamageTakenMult().unmodify(nodeId);
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI e = Global.getCombatEngine();
        return e != null ? e.getPlayerShip() : null;
    }
}
