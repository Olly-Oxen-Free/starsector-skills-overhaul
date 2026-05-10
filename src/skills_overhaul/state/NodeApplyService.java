package skills_overhaul.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.json.JSONException;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.effect.EffectRegistry;
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
                    for (JSONObject effect : node.getEffects()) {
                        String type;
                        try { type = effect.getString("type"); }
                        catch (JSONException e) { continue; }
                        EffectHandler h = EffectRegistry.get(type);
                        if (h == null) continue;
                        try { h.apply(node.getId(), effect); count++; }
                        catch (RuntimeException ex) {
                            Global.getLogger(NodeApplyService.class).warn(
                                    "reapply failed for node=" + node.getId()
                                            + " type=" + type + ": " + ex);
                        }
                    }
                }
            }
        }
        Global.getLogger(NodeApplyService.class).info(
                "[skills_overhaul] re-applied " + count + " owned-node effects on load");
    }

    private NodeApplyService() {}
}
