package skills_overhaul.effect.handlers.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.systems.SystemsCombatPlugin;

public final class SOSystemsEMPSpecialistHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.add(nodeId);
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        // No direct outgoing-EMP-damage stat; plugin handles debuff on hit targets via ACTIVE_NODES
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        SystemsCombatPlugin.ACTIVE_NODES.remove(nodeId);
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI engine = Global.getCombatEngine();
        return engine != null ? engine.getPlayerShip() : null;
    }
}
