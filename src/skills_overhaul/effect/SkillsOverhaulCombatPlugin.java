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
 * MutableShipStatsAPI is rebuilt per combat, so static ship-stat passives
 * (SO_piloted_stat) must be re-applied here each battle; purchase-time
 * application cannot stick. Non-stat effect types are handled by their own
 * combat plugins via ACTIVE_NODES.
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

    private final skills_overhaul.effect.handlers.SOPilotedStatHandler pilotedStat =
            new skills_overhaul.effect.handlers.SOPilotedStatHandler();

    private void applyPilotSkillsTo(ShipAPI ship) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        MutableShipStatsAPI stats = ship.getMutableStats();

        int applied = 0;
        for (TreeDef def : TreeRegistry.getTrees(Tree.PILOT)) {
            for (Node n : def.getNodes()) {
                if (!mem.contains(SkillMemory.purchasedKey(n.getId()))) continue;
                if (!mem.getBoolean(SkillMemory.purchasedKey(n.getId()))) continue;
                for (JSONObject fx : n.getEffects()) {
                    if (!NodeEffectApplier.isEffectActive(n, fx)) continue;
                    if (applyShipStatEffect(stats, n.getId(), fx)) applied++;
                }
            }
        }
        log.info("[SkillsOverhaul] combat init: applied " + applied + " ship-stat effects to " + ship.getHullSpec().getHullId());
    }

    private boolean applyShipStatEffect(MutableShipStatsAPI s, String nodeId, JSONObject fx) {
        String type = fx.optString("type");
        if (type.equals("SO_piloted_stat")) {
            // MutableShipStatsAPI is rebuilt per combat, so static passives must
            // be re-applied here — purchase-time application cannot stick.
            pilotedStat.applyTo(s, nodeId, fx);
            return true;
        }
        // Other types are handled by their own combat plugins via ACTIVE_NODES.
        return false;
    }
}
