package skills_overhaul.effect.handlers.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.pilot.defense.DefenseCombatPlugin;

/**
 * Unbreakable (T4) — registers DefenseCombatPlugin to attach the
 * UnbreakableArmorListener at combat init. The listener implements the
 * per-cell armor floor + 8s block + 15s cooldown logic.
 */
public final class SODefenseUnbreakableHandler implements EffectHandler {
    @Override
    public void apply(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.add(nodeId);
        // Listener attached by DefenseCombatPlugin.advance() init block.
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        DefenseCombatPlugin.ACTIVE_NODES.remove(nodeId);
        // Listener removal is best-effort — only unregisters on next combat init.
        ShipAPI ship = getPlayerShip();
        if (ship == null) return;
        ship.removeListenerOfClass(skills_overhaul.pilot.defense.UnbreakableArmorListener.class);
    }

    private ShipAPI getPlayerShip() {
        com.fs.starfarer.api.combat.CombatEngineAPI e = Global.getCombatEngine();
        return e != null ? e.getPlayerShip() : null;
    }
}
