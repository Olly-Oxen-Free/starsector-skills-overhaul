package skills_overhaul.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
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
 * before first advance(). Player ship may be null in init() so defer to advance(), which
 * also moves the passives if the player transfers command to another ship.
 *
 * MutableShipStatsAPI is rebuilt per combat, so static ship-stat passives
 * (SO_piloted_stat) must be re-applied here each battle; purchase-time
 * application cannot stick. Non-stat effect types are handled by their own
 * combat plugins via ACTIVE_NODES.
 */
public class SkillsOverhaulCombatPlugin extends BaseEveryFrameCombatPlugin {
    private static final Logger log = Global.getLogger(SkillsOverhaulCombatPlugin.class);
    /** Ship currently carrying the pilot passives; follows the player on ship change. */
    private ShipAPI appliedTo = null;

    @Override
    public void init(CombatEngineAPI engine) {
        appliedTo = null;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        // Pilot skills come from the campaign character; skip title-screen/mission battles.
        if (!engine.isInCampaign() && !engine.isInCampaignSim()) return;
        ShipAPI player = engine.getPlayerShip();
        if (player == null || player == appliedTo || player.isShuttlePod()) return;
        // Player changed ship (transfer command): move the passives with them.
        if (appliedTo != null) applyPilotSkills(appliedTo, false);
        applyPilotSkills(player, true);
        appliedTo = player;
    }

    private void applyPilotSkills(ShipAPI ship, boolean apply) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        int applied = 0;
        for (TreeDef def : TreeRegistry.getTrees(Tree.PILOT)) {
            for (Node n : def.getNodes()) {
                if (!mem.contains(SkillMemory.purchasedKey(n.getId()))) continue;
                if (!mem.getBoolean(SkillMemory.purchasedKey(n.getId()))) continue;
                for (JSONObject fx : n.getEffects()) {
                    // MutableShipStatsAPI is rebuilt per combat, so static passives must
                    // be re-applied here — purchase-time application cannot stick.
                    // Other types are handled by their own combat plugins via ACTIVE_NODES.
                    EffectHandler h = EffectRegistry.get(fx.optString("type"));
                    if (!(h instanceof PilotShipStatEffect)) continue;
                    if (!apply) {
                        ((PilotShipStatEffect) h).removeFrom(ship, n.getId(), fx);
                        continue;
                    }
                    if (!NodeEffectApplier.isEffectActive(n, fx)) continue;
                    ((PilotShipStatEffect) h).applyTo(ship, n.getId(), fx);
                    applied++;
                }
            }
        }
        if (apply) log.info("[SkillsOverhaul] combat init: applied " + applied + " ship-stat effects to " + ship.getHullSpec().getHullId());
    }
}
