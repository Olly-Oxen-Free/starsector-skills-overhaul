package skills_overhaul.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;

import java.util.List;

/**
 * Cross-lane fix for the ACTIVE_NODES save/reload bug.
 *
 * Each pilot CombatPlugin (Defense/Gunnery/Maneuvering/Systems) owns a
 * static {@code ACTIVE_NODES} set populated by EffectHandler.apply() at
 * node-purchase time. The set is JVM-static, so save/reload (JVM restart)
 * empties it; the corresponding handlers never re-fire automatically and
 * all plugin-gated skills go silently dormant until the player respecs.
 *
 * Source of truth = {@code SkillMemory.purchasedKey(nodeId)} in MemoryAPI
 * (already persistent through vanilla save).
 *
 * Fix: on every game load, scan all trees, find purchased nodes, invoke
 * each effect handler's {@code apply()} so static sets / hullmod state /
 * memory keys get re-seeded.
 */
public final class NodeApplyService {

    public static void reapplyAllOwned() {
        // Statics survive across save loads in the same JVM. Without this
        // reset, loading save B after save A carries A's active nodes,
        // recipe unlocks, cap multipliers and command-field params into B.
        resetTransientStatics();

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int count = 0;
        for (Tree tab : Tree.values()) {
            List<TreeDef> trees;
            try { trees = TreeRegistry.getTrees(tab); }
            catch (Exception e) { continue; }
            if (trees == null) continue;
            for (TreeDef def : trees) {
                for (Node node : def.getNodes()) {
                    if (!Boolean.TRUE.equals(mem.get(SkillMemory.purchasedKey(node.getId())))) continue;
                    // Route through NodeEffectApplier so T0-lane/oath gating
                    // is honored — applying every effect unconditionally would
                    // grant inactive-lane and off-oath bonuses on load.
                    try {
                        skills_overhaul.effect.NodeEffectApplier.apply(node);
                        count++;
                    } catch (RuntimeException ex) {
                        Global.getLogger(NodeApplyService.class).warn(
                                "reapply failed for node=" + node.getId() + ": " + ex);
                    }
                }
            }
        }
        Global.getLogger(NodeApplyService.class).info(
                "[skills_overhaul] re-applied " + count + " owned nodes on load");
    }

    /** Clear every JVM-static registry that purchase-time handlers populate. */
    private static void resetTransientStatics() {
        skills_overhaul.pilot.gunnery.GunneryCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.pilot.maneuvering.ManeuveringCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.pilot.systems.SystemsCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.pilot.defense.DefenseCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.zealot.ZealotCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.commandant.CommandantCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.effect.handlers.corsair.CorsairCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.artificer.ArtificerCombatPlugin.ACTIVE_NODES.clear();
        skills_overhaul.collective.CollectiveActiveNodes.ACTIVE.clear();
        skills_overhaul.commandant.CommandFieldService.clearAllParams();
        skills_overhaul.supply.SupplyBufferService.clearCapMultipliers();
        skills_overhaul.artificer.forge.ForgeRecipeRegistry.resetUnlocks();
        skills_overhaul.artificer.forge.FreeDeployQuotaTracker.reset();
        skills_overhaul.artificer.AutomatedShipPointService.reset();
        skills_overhaul.effect.handlers.corsair.BountyBoardListener.active = false;
        skills_overhaul.effect.handlers.corsair.BountyBoardListener.weaponDropMult = 1f;
        skills_overhaul.commandant.FieldCommandsService.active = false;
        // Salvage multipliers are keyed by node id and have no bulk clear.
        for (Tree tab : Tree.values()) {
            List<TreeDef> trees;
            try { trees = TreeRegistry.getTrees(tab); }
            catch (Exception e) { continue; }
            for (TreeDef def : trees) {
                for (Node node : def.getNodes()) {
                    skills_overhaul.effect.handlers.corsair.PostBattleSalvageMultiplierListener.unregister(node.getId());
                }
            }
        }
    }

    private NodeApplyService() {}
}
