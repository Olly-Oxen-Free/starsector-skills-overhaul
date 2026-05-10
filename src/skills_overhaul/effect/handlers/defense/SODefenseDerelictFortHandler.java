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
        // Spec line 60: −20% d-mod negatives. No per-d-mod stat key in API; use
        // counter-buff approximation on commonly-impacted stats. Each d-mod
        // typically costs ~3%; apply 20% of that as positive (=0.6% per d-mod,
        // cap 5).
        float counter = Math.min(5, dmodCount) * 0.006f;
        s.getMaxSpeed().modifyPercent(nodeId + "_dmod_counter", counter * 100f);
        s.getBallisticRoFMult().modifyMult(nodeId + "_dmod_counter", 1f + counter);
        s.getEnergyRoFMult().modifyMult(nodeId + "_dmod_counter", 1f + counter);
        s.getCRLossPerSecondPercent().modifyMult(nodeId + "_dmod_counter", 1f - counter);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.remove(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        MutableShipStatsAPI s = ship.getMutableStats();
        s.getArmorBonus().unmodifyPercent(nodeId);
        s.getHullBonus().unmodifyPercent(nodeId);
        s.getMaxSpeed().unmodifyPercent(nodeId + "_dmod_counter");
        s.getBallisticRoFMult().unmodify(nodeId + "_dmod_counter");
        s.getEnergyRoFMult().unmodify(nodeId + "_dmod_counter");
        s.getCRLossPerSecondPercent().unmodify(nodeId + "_dmod_counter");
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
