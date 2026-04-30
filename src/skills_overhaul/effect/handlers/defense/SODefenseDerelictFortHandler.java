package skills_overhaul.effect.handlers.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.defense.DefenseCombatPlugin;

public final class SODefenseDerelictFortHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.add(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        int dmodCount = countDMods(ship);
        if (dmodCount == 0) return;
        float bonus = Math.min(5, dmodCount) * 0.03f;
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getArmorBonus().modifyPercent(nodeId, bonus * 100f);
        s.getHullBonus().modifyPercent(nodeId, bonus * 100f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.remove(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getArmorBonus().unmodifyPercent(nodeId);
        s.getHullBonus().unmodifyPercent(nodeId);
    }

    private int countDMods(ShipAPI ship) {
        int count = 0;
        for (String hullmodId : ship.getVariant().getHullMods()) {
            try {
                HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullmodId);
                if (spec != null && spec.hasTag("dmod")) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI e = Global.getCombatEngine();
        return e != null ? e.getPlayerShip() : null;
    }
}
