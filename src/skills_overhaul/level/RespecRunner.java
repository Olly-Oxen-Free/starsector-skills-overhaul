package skills_overhaul.level;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import skills_overhaul.basics.ActiveLaneService;
import skills_overhaul.effect.NodeEffectApplier;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;

import java.util.*;

public final class RespecRunner {

    public static RespecService.Result run(Tree tree) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        // Paid respec with no story point: refuse before mutating anything
        // (spendStoryPoints clamps the pool at 0, so the respec would be free).
        Map<String,Object> used = new HashMap<String,Object>();
        if (mem.contains(SkillMemory.respecUsedKey(tree)) && mem.getBoolean(SkillMemory.respecUsedKey(tree)))
            used.put(SkillMemory.respecUsedKey(tree), true);
        if (RespecService.costFor(used, tree) > 0 && Global.getSector().getPlayerStats().getStoryPoints() < 1) return null;

        // Collect previously-owned nodes BEFORE respec clears their purchased flags
        List<Node> ownedBefore = new ArrayList<Node>();
        for (TreeDef def : TreeRegistry.getTrees(tree)) {
            for (Node n : def.getNodes()) {
                String k = SkillMemory.purchasedKey(n.getId());
                if (mem.contains(k) && mem.getBoolean(k)) ownedBefore.add(n);
            }
        }

        // Snapshot minimal state: points, freeUni, migUni per tree, purchased flags for this tree's nodes, records for this tree
        Map<String,Object> m = new HashMap<String,Object>();
        for (Tree t : Tree.values()) {
            if (mem.contains(SkillMemory.pointsKey(t)))
                m.put(SkillMemory.pointsKey(t), mem.getFloat(SkillMemory.pointsKey(t)));
            if (mem.contains(SkillMemory.migUniKey(t)))
                m.put(SkillMemory.migUniKey(t), mem.getFloat(SkillMemory.migUniKey(t)));
        }
        if (mem.contains(SkillMemory.freeUniKey()))
            m.put(SkillMemory.freeUniKey(), mem.getFloat(SkillMemory.freeUniKey()));
        if (mem.contains(SkillMemory.respecUsedKey(tree)) && mem.getBoolean(SkillMemory.respecUsedKey(tree)))
            m.put(SkillMemory.respecUsedKey(tree), true);
        for (Node n : ownedBefore) {
            String pk = SkillMemory.purchasedKey(n.getId());
            if (mem.contains(pk) && mem.getBoolean(pk)) m.put(pk, true);
            for (String suffix : new String[]{".tl", ".fu", ".mg"}) {
                String rk = RespecService.RECORD_PREFIX + n.getId() + suffix;
                if (mem.contains(rk)) m.put(rk, mem.getFloat(rk));
            }
        }

        RespecService.Result r = RespecService.respecTree(m, tree);

        // Write back
        for (Tree t : Tree.values()) {
            if (m.containsKey(SkillMemory.pointsKey(t)))
                mem.set(SkillMemory.pointsKey(t), ((Number) m.get(SkillMemory.pointsKey(t))).floatValue());
            if (m.containsKey(SkillMemory.migUniKey(t)))
                mem.set(SkillMemory.migUniKey(t), ((Number) m.get(SkillMemory.migUniKey(t))).floatValue());
        }
        if (m.containsKey(SkillMemory.freeUniKey()))
            mem.set(SkillMemory.freeUniKey(), ((Number) m.get(SkillMemory.freeUniKey())).floatValue());
        if (Boolean.TRUE.equals(m.get(SkillMemory.respecUsedKey(tree))))
            mem.set(SkillMemory.respecUsedKey(tree), true);
        for (Node n : ownedBefore) {
            mem.unset(SkillMemory.purchasedKey(n.getId()));
            for (String suffix : new String[]{".tl", ".fu", ".mg"}) {
                mem.unset(RespecService.RECORD_PREFIX + n.getId() + suffix);
            }
        }

        // Drop T0 nodes from active basics
        for (Node n : ownedBefore) {
            if (n.getY() == 0) ActiveLaneService.onUnpurchased(mem, n.getId());
        }

        // Remove effects
        for (Node n : ownedBefore) NodeEffectApplier.remove(n);

        // Charge SP (verbose=false, no text panel for compiles-only stub)
        if (r.storyPointsCharged > 0) {
            MutableCharacterStatsAPI stats = Global.getSector().getPlayerPerson().getStats();
            stats.spendStoryPoints(r.storyPointsCharged, false, null, false, "Skills Overhaul respec");
        }

        return r;
    }
}
