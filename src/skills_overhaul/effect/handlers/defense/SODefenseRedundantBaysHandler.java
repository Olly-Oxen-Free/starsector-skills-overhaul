package skills_overhaul.effect.handlers.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.effect.ShipStatEffect;
import skills_overhaul.pilot.defense.DefenseCombatPlugin;

public final class SODefenseRedundantBaysHandler implements EffectHandler, ShipStatEffect {
    @Override
    public void apply(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.add(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        applyTo(ship.getMutableStats(), ship, nodeId, params);
    }

    @Override
    public void applyTo(MutableShipStatsAPI s, ShipAPI ship, String nodeId, JSONObject params) {
        s.getFighterRefitTimeMult().modifyMult(nodeId, 0.85f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.remove(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        ship.getMutableStats().getFighterRefitTimeMult().unmodify(nodeId);
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI e = Global.getCombatEngine();
        return e != null ? e.getPlayerShip() : null;
    }
}
