package skills_overhaul.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import skills_overhaul.level.XPCurve;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.awt.Color;
import java.util.*;

/**
 * Owns the entire character-tab region. Three regions:
 *   - top tab strip (PILOT / COMMANDER / ADMIN)
 *   - left rail (portrait + SP count + Respec/Confirm + pool labels)
 *   - main canvas (3-lane grid of 48×48 node buttons)
 *   - bottom bar (tree name + XP progress + level/points)
 */
public final class CharacterTabPanelPlugin extends BaseCustomUIPanelPlugin {

    static final float NODE_SIZE        = 48f;
    static final float TAB_BAR_H        = 30f;
    static final float LEFT_RAIL_W      = 130f;
    static final float BOTTOM_BAR_H     = 40f;
    static final float PAD              = 14f;

    private CustomPanelAPI panel;

    // Active tree + per-tree pending queues (preserved across tree-tab switches? No —
    // spec §4.3 "Tab switch: clears pending for the tab being left".)
    Tree currentTree = Tree.PILOT;
    final Map<Tree, Set<String>> pendingByTree = new EnumMap<Tree, Set<String>>(Tree.class);
    final List<ConnectionLineDrawer.Edge> edges = new ArrayList<ConnectionLineDrawer.Edge>();
    final Map<String, float[]> nodeCenters = new HashMap<String, float[]>(); // id -> {cx, cy}

    public CharacterTabPanelPlugin() {
        for (Tree t : Tree.values()) pendingByTree.put(t, new LinkedHashSet<String>());
    }

    public void attach(CustomPanelAPI panel) {
        this.panel = panel;
        rebuild();
    }

    /** Clears the panel and rebuilds from current state (currentTree + pendingByTree). */
    public void rebuild() {
        if (panel == null) return;
        edges.clear();
        nodeCenters.clear();
        // Starsector doesn't expose a removeAll on CustomPanelAPI; add a fresh UIElement
        // each rebuild. The previous UIElement remains but is covered.  TooltipMakerAPI
        // elements stack; for stable behavior we add a single top-level UIElement per
        // rebuild that paints over.  If buildup becomes a problem we'll reset via
        // parent.removeComponent + recreate, but in practice repaints are infrequent.
        float w = panel.getPosition().getWidth();
        float h = panel.getPosition().getHeight();
        TooltipMakerAPI root = panel.createUIElement(w, h, false);

        paintTabStrip(root, w);
        paintLeftRail(root, w, h);
        paintMainCanvas(root, w, h);
        paintBottomBar(root, w, h);

        panel.addUIElement(root).inTL(0f, 0f);
    }

    private void paintTabStrip(TooltipMakerAPI tip, float w) {
        for (Tree t : Tree.values()) {
            String label = t.key.toUpperCase();
            tip.addButton(label, "__tab__" + t.name(), 100f, TAB_BAR_H, 4f);
        }
    }

    private void paintLeftRail(TooltipMakerAPI tip, float w, float h) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int sp = Global.getSector().getPlayerStats().getStoryPoints();

        tip.addSectionHeading("Character", Color.GRAY, new Color(0, 0, 0, 0), com.fs.starfarer.api.ui.Alignment.MID, 6f);
        tip.addPara("Story points: " + sp, 4f);

        Set<String> pending = pendingByTree.get(currentTree);
        if (pending.isEmpty()) {
            tip.addButton("Respec (1 SP)", "__respec__", LEFT_RAIL_W, 24f, 8f);
        } else {
            int costSum = sumPendingCost(pending);
            tip.addButton("Confirm (" + costSum + " pts)", "__confirm__", LEFT_RAIL_W, 24f, 8f);
        }

        float freeUni = mem.contains(SkillMemory.freeUniKey()) ? mem.getFloat(SkillMemory.freeUniKey()) : 0f;
        float migPool = mem.contains("$skills.points.universal_migrated_unallocated")
                ? mem.getFloat("$skills.points.universal_migrated_unallocated") : 0f;
        tip.addPara("Free Uni: " + (int) freeUni, 4f);
        tip.addPara("Mig Pool: " + (int) migPool, 2f);
    }

    private void paintMainCanvas(TooltipMakerAPI tip, float w, float h) {
        TreeGridBuilder.build(tip, this, currentTree);
    }

    private void paintBottomBar(TooltipMakerAPI tip, float w, float h) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        Map<String,Object> m = new HashMap<String,Object>();
        String lk = SkillMemory.levelKey(currentTree);
        String pk = SkillMemory.pointsKey(currentTree);
        if (mem.contains(lk)) m.put(lk, mem.getFloat(lk));
        if (mem.contains(pk)) m.put(pk, mem.getFloat(pk));
        int level = SkillMemory.getLevel(m, currentTree);
        int pts = SkillMemory.getPoints(m, currentTree);
        long xp = (long) SkillMemory.getXP(mem, currentTree);
        long req = XPCurve.xpForLevel(level + 1);
        long cur = xp - XPCurve.cumulativeXP(level);
        if (cur < 0) cur = 0;

        tip.addSpacer(8f);
        tip.addPara(currentTree.key.toUpperCase() + "   L" + level
                + "   " + cur + " / " + req + " XP   (" + pts + " pts)", 4f);
    }

    @Override
    public void render(float alphaMult) {
        if (edges.isEmpty()) return;
        ConnectionLineDrawer.draw(edges, alphaMult);
    }

    int sumPendingCost(Set<String> pending) {
        int s = 0;
        skills_overhaul.tree.TreeDef def =
                skills_overhaul.tree.TreeRegistry.get(currentTree);
        for (String id : pending) {
            skills_overhaul.tree.Node n = def.getNode(id);
            if (n != null) s += n.getCost();
        }
        return s;
    }

    @Override
    public void buttonPressed(Object id) {
        if (!(id instanceof String)) return;
        String s = (String) id;

        // Tree tab switch: id = "__tab__PILOT" | "__tab__COMMANDER" | "__tab__ADMIN"
        if (s.startsWith("__tab__")) {
            Tree next = Tree.valueOf(s.substring("__tab__".length()));
            if (next == currentTree) return;
            // Clear pending for the tab we're leaving (spec §4.3 "vanilla behavior").
            pendingByTree.get(currentTree).clear();
            currentTree = next;
            rebuild();
            return;
        }

        // Respec
        if ("__respec__".equals(s)) {
            skills_overhaul.level.RespecRunner.run(currentTree);
            rebuild();
            return;
        }

        // Confirm
        if ("__confirm__".equals(s)) {
            commitPending();
            rebuild();
            return;
        }

        // Node click: toggle pending.
        Set<String> pending = pendingByTree.get(currentTree);
        if (pending.contains(s)) {
            // Already pending — remove. But if any other pending node depends on s,
            // removing breaks their unlockability; simplest rule: remove s and also
            // remove anything that transitively depends on s.
            removePendingWithDependents(s, pending);
            rebuild();
            return;
        }
        // Is it a valid unlockable?
        com.fs.starfarer.api.campaign.rules.MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        java.util.Map<String,Object> snap = new java.util.HashMap<String,Object>();
        skills_overhaul.tree.TreeDef def = skills_overhaul.tree.TreeRegistry.get(currentTree);
        for (skills_overhaul.tree.Node n : def.getNodes()) {
            String k = skills_overhaul.state.SkillMemory.purchasedKey(n.getId());
            if (mem.contains(k) && mem.getBoolean(k)) snap.put(k, Boolean.TRUE);
        }
        String pk = skills_overhaul.state.SkillMemory.pointsKey(currentTree);
        if (mem.contains(pk)) snap.put(pk, mem.getFloat(pk));
        if (!skills_overhaul.tree.PrereqValidator.canPurchaseWithPending(def, snap, pending, s)) {
            return; // owned, locked, or already pending
        }
        // Budget check: can the player afford cost-sum-so-far + this node?
        int pts = skills_overhaul.state.SkillMemory.getPoints(snap, currentTree);
        skills_overhaul.tree.Node clicked = def.getNode(s);
        if (clicked == null) return;
        if (sumPendingCost(pending) + clicked.getCost() > pts) {
            // Over budget — flash red (approximated via log for now; spec §4.5).
            Global.getLogger(CharacterTabPanelPlugin.class).warn(
                    "[SkillsOverhaul/UI] over budget: can't pend " + s);
            return;
        }
        pending.add(s);
        rebuild();
    }

    @Override
    public void processInput(java.util.List<com.fs.starfarer.api.input.InputEventAPI> events) {
        if (events == null) return;
        for (com.fs.starfarer.api.input.InputEventAPI e : events) {
            if (e.isConsumed()) continue;
            if (!e.isKeyDownEvent()) continue;
            if (e.getEventValue() == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                boolean anyPending = false;
                for (Set<String> pending : pendingByTree.values()) if (!pending.isEmpty()) { anyPending = true; break; }
                if (anyPending) {
                    for (Set<String> pending : pendingByTree.values()) pending.clear();
                    e.consume();
                    rebuild();
                    return;
                }
            }
        }
    }

    private void commitPending() {
        Set<String> pending = pendingByTree.get(currentTree);
        if (pending.isEmpty()) return;
        skills_overhaul.tree.TreeDef def = skills_overhaul.tree.TreeRegistry.get(currentTree);
        java.util.List<String> order;
        try {
            order = skills_overhaul.tree.PendingPurchaseTopoSort.sort(def, pending);
        } catch (Throwable t) {
            Global.getLogger(CharacterTabPanelPlugin.class).warn(
                    "[SkillsOverhaul/UI] topo sort failed, clearing pending", t);
            pending.clear();
            return;
        }
        for (String nodeId : order) {
            boolean ok = skills_overhaul.level.GameSpend.purchase(currentTree, nodeId);
            if (!ok) {
                Global.getLogger(CharacterTabPanelPlugin.class).warn(
                        "[SkillsOverhaul/UI] purchase failed for " + nodeId + "; stopping commit");
                break;
            }
        }
        pending.clear();
    }

    private void removePendingWithDependents(String removed, Set<String> pending) {
        skills_overhaul.tree.TreeDef def = skills_overhaul.tree.TreeRegistry.get(currentTree);
        Set<String> toRemove = new java.util.HashSet<String>();
        toRemove.add(removed);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String id : new java.util.ArrayList<String>(pending)) {
                if (toRemove.contains(id)) continue;
                skills_overhaul.tree.Node n = def.getNode(id);
                if (n == null) continue;
                for (String pre : n.getPrereqs()) {
                    if (toRemove.contains(pre)) { toRemove.add(id); changed = true; break; }
                }
            }
        }
        pending.removeAll(toRemove);
    }
}
