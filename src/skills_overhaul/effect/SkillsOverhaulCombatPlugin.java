package skills_overhaul.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;
import skills_overhaul.state.Tree;

import java.util.List;

/**
 * Applies ship-stat effects from purchased skill nodes to the piloted ship at combat start.
 *
 * Registered via data/config/settings.json "plugins" key. init() fires once per combat,
 * before first advance(). Player ship may be null in init() so defer to first advance tick.
 *
 * Design: handlers classes (PilotedManeuverabilityHandler etc.) still fire on purchase for
 * campaign-side logging. This plugin is the real combat-time application: walk purchased
 * nodes, match effect types, modifyMult on player ship stats.
 *
 * Only piloted_maneuverability is wired to real stats in v1 — rest log STUB and will be
 * filled per effect type post-playtest.
 */
public class SkillsOverhaulCombatPlugin extends BaseEveryFrameCombatPlugin {
    private static final Logger log = Global.getLogger(SkillsOverhaulCombatPlugin.class);
    private boolean applied = false;

    @Override
    public void init(CombatEngineAPI engine) {
        applied = false;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (applied) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        ShipAPI player = engine.getPlayerShip();
        if (player == null) return;
        applyPilotSkillsTo(player);
        applied = true;
    }

    private void applyPilotSkillsTo(ShipAPI ship) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        MutableShipStatsAPI stats = ship.getMutableStats();

        int applied = 0;
        for (TreeDef def : TreeRegistry.getTrees(Tree.PILOT)) {
            for (Node n : def.getNodes()) {
                if (!mem.contains(SkillMemory.purchasedKey(n.getId()))) continue;
                if (!mem.getBoolean(SkillMemory.purchasedKey(n.getId()))) continue;
                for (JSONObject fx : n.getEffects()) {
                    if (applyShipStatEffect(stats, n.getId(), fx)) applied++;
                }
            }
        }
        log.info("[SkillsOverhaul] combat init: applied " + applied + " ship-stat effects to " + ship.getHullSpec().getHullId());
    }

    private boolean applyShipStatEffect(MutableShipStatsAPI s, String nodeId, JSONObject fx) {
        String type = fx.optString("type");
        float amt = (float) fx.optDouble("amount", 0);
        if (type.equals("piloted_maneuverability")) {
            s.getMaxTurnRate().modifyMult(nodeId, 1f + amt);
            s.getTurnAcceleration().modifyMult(nodeId, 1f + amt);
            s.getAcceleration().modifyMult(nodeId, 1f + amt);
            return true;
        }
        // Unhandled types fall through — purchase-time handler already logged STUB.
        return false;
    }
}
