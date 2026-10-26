package skills_overhaul.level;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.basics.ActiveLaneService;
import skills_overhaul.effect.NodeEffectApplier;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;
import skills_overhaul.ui.SkillUISounds;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameSpend {
    public static boolean purchase(Tree tree, String nodeId) {
        TreeDef def = TreeRegistry.findTreeOf(tree, nodeId);
        if (def == null) return false;
        Node n = def.getNode(nodeId);
        if (n == null) return false;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Map<String,Object> m = snapshot(mem, tree);

        if (!PrereqValidator.canPurchase(def, m, nodeId)) return false;

        SpendService.Result r = SpendService.spend(m, tree, nodeId, n.getCost());
        if (!r.success) return false;

        int totalMig = r.fromMigrationToPilot + r.fromMigrationToCommander
                     + r.fromMigrationToAdmin;
        RespecService.recordPurchase(m, nodeId, r.fromTreeLocked, r.fromFreeUni, totalMig);

        writeBack(mem, m, nodeId);
        if (n.getY() == 0) {
            ActiveLaneService.onPurchased(mem, nodeId);
        }
        NodeEffectApplier.apply(n);
        SkillUISounds.playTierSkillSound(tree, n.getTier(), 1f);
        return true;
    }

    /**
     * Buys every node of {@code nodeIds} or none. Dry-runs the batch with
     * {@link PurchasePlan} on a whole-tab snapshot; on failure writes nothing, plays
     * no sound, and returns the failing node and reason. On success writes each node
     * in plan order through the same path as {@link #purchase} (without its sound).
     * A throw from {@link NodeEffectApplier#apply} is logged and the batch continues;
     * points already written stay spent.
     */
    public static PurchasePlan.Result commit(Tree tab, Collection<String> nodeIds) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        List<TreeDef> defs = TreeRegistry.getTrees(tab);
        Map<String,Object> m = snapshot(mem, tab);

        PurchasePlan.Result plan = PurchasePlan.dryRun(m, tab, defs, nodeIds);
        if (!plan.success) return plan;

        for (String nodeId : plan.order) {
            Node n = TreeRegistry.findNode(tab, nodeId);
            SpendService.Result r = SpendService.spend(m, tab, nodeId, n.getCost());
            if (!r.success) {
                // Unreachable after a successful dry run on the same snapshot.
                Global.getLogger(GameSpend.class).error(
                        "[SkillsOverhaul] commit: spend failed after dry run for " + nodeId
                        + ": " + r.reason);
                return PurchasePlan.Result.fail(nodeId, PurchasePlan.Reason.BUDGET,
                        PurchasePlan.shortBy(r, n.getCost()), m);
            }
            int totalMig = r.fromMigrationToPilot + r.fromMigrationToCommander
                         + r.fromMigrationToAdmin;
            RespecService.recordPurchase(m, nodeId, r.fromTreeLocked, r.fromFreeUni, totalMig);

            writeBack(mem, m, nodeId);
            if (n.getY() == 0) {
                ActiveLaneService.onPurchased(mem, nodeId);
            }
            try {
                NodeEffectApplier.apply(n);
            } catch (Throwable t) {
                Global.getLogger(GameSpend.class).error(
                        "[SkillsOverhaul] commit: effect apply failed for " + nodeId, t);
            }
        }
        return plan;
    }

    /** Points, pools, pool lock and the purchased flags of every node of the tab. */
    private static Map<String,Object> snapshot(MemoryAPI mem, Tree tab) {
        Map<String,Object> m = new HashMap<String,Object>();
        for (Tree t : Tree.values()) {
            m.put(SkillMemory.pointsKey(t), (float) getFloat(mem, SkillMemory.pointsKey(t)));
            m.put(SkillMemory.migUniKey(t), (float) getFloat(mem, SkillMemory.migUniKey(t)));
        }
        m.put(SkillMemory.freeUniKey(), (float) getFloat(mem, SkillMemory.freeUniKey()));
        m.put(SpendService.POOL_UNALLOC, (float) getFloat(mem, SpendService.POOL_UNALLOC));
        if (mem.contains(SpendService.POOL_LOCK_KEY)) {
            m.put(SpendService.POOL_LOCK_KEY, mem.getString(SpendService.POOL_LOCK_KEY));
        }
        // PrereqValidator builds its owned-set from these flags; without them
        // every node with a prereq fails validation against the snapshot.
        for (TreeDef def : TreeRegistry.getTrees(tab)) {
            for (String pk : purchasedKeys(def)) {
                if (mem.getBoolean(pk)) m.put(pk, Boolean.TRUE);
            }
        }
        return m;
    }

    /** Memory keys of the purchased-flags PrereqValidator reads for this tree. */
    static java.util.List<String> purchasedKeys(TreeDef def) {
        java.util.List<String> keys = new java.util.ArrayList<String>();
        for (Node n : def.getNodes()) {
            keys.add(SkillMemory.purchasedKey(n.getId()));
        }
        return keys;
    }

    private static void writeBack(MemoryAPI mem, Map<String,Object> m, String nodeId) {
        for (Tree t : Tree.values()) {
            mem.set(SkillMemory.pointsKey(t), ((Number) m.get(SkillMemory.pointsKey(t))).floatValue());
            mem.set(SkillMemory.migUniKey(t), ((Number) m.get(SkillMemory.migUniKey(t))).floatValue());
        }
        mem.set(SkillMemory.freeUniKey(), ((Number) m.get(SkillMemory.freeUniKey())).floatValue());
        mem.set(SpendService.POOL_UNALLOC, ((Number) m.get(SpendService.POOL_UNALLOC)).floatValue());
        Object lock = m.get(SpendService.POOL_LOCK_KEY);
        if (lock == null) mem.unset(SpendService.POOL_LOCK_KEY);
        else mem.set(SpendService.POOL_LOCK_KEY, (String) lock);
        Object purchased = m.get(SkillMemory.purchasedKey(nodeId));
        if (purchased != null) mem.set(SkillMemory.purchasedKey(nodeId), (Boolean) purchased);
        for (String suffix : new String[]{".tl", ".fu", ".mg"}) {
            String rk = RespecService.RECORD_PREFIX + nodeId + suffix;
            Object v = m.get(rk);
            if (v != null) mem.set(rk, ((Number) v).floatValue());
        }
    }

    private static float getFloat(MemoryAPI mem, String key) {
        return mem.contains(key) ? mem.getFloat(key) : 0f;
    }
}
