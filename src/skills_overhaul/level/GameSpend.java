package skills_overhaul.level;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.effect.NodeEffectApplier;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;

import java.util.HashMap;
import java.util.Map;

public final class GameSpend {
    public static boolean purchase(Tree tree, String nodeId) {
        TreeDef def = TreeRegistry.get(tree);
        Node n = def.getNode(nodeId);
        if (n == null) return false;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Map<String,Object> m = snapshot(mem);

        if (!PrereqValidator.canPurchase(def, m, nodeId)) return false;

        SpendService.Result r = SpendService.spend(m, tree, nodeId, n.getCost());
        if (!r.success) return false;

        int totalMig = r.fromMigrationToPilot + r.fromMigrationToCommander + r.fromMigrationToAdmin;
        RespecService.recordPurchase(m, nodeId, r.fromTreeLocked, r.fromFreeUni, totalMig);

        writeBack(mem, m, nodeId);
        NodeEffectApplier.apply(n);
        return true;
    }

    private static Map<String,Object> snapshot(MemoryAPI mem) {
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
        return m;
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
