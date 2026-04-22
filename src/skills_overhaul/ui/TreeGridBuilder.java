package skills_overhaul.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;

import java.awt.Color;
import java.util.*;

/**
 * Lays out one tree's nodes on a 3-lane grid with hybrid columns.
 * Column layout: [lane0 L M R] [hybrid-01] [lane1 L M R] [hybrid-12] [lane2 L M R]
 * Row layout:    one row per distinct tier value ascending + one capstone row at the bottom.
 */
public final class TreeGridBuilder {

    private static final float NODE = CharacterTabPanelPlugin.NODE_SIZE;
    private static final float GAP_X = 4f;
    private static final float GAP_Y = 6f;
    private static final float HYBRID_COL_W = NODE + 8f;
    private static final float LANE_COL_W  = 3 * NODE + 2 * GAP_X;

    private TreeGridBuilder() {}

    public static void build(TooltipMakerAPI tip, CharacterTabPanelPlugin host, Tree t) {
        TreeDef def = TreeRegistry.get(t);
        List<String> lanes = def.getLanes();

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Set<String> owned = new HashSet<String>();
        Map<String,Object> memSnapshot = new HashMap<String,Object>();
        for (Node n : def.getNodes()) {
            String k = SkillMemory.purchasedKey(n.getId());
            if (mem.contains(k) && mem.getBoolean(k)) {
                owned.add(n.getId());
                memSnapshot.put(k, Boolean.TRUE);
            }
        }
        Set<String> pending = host.pendingByTree.get(t);

        SortedSet<Integer> tiers = new TreeSet<Integer>();
        for (Node n : def.getNodes()) if (!n.isCapstone()) tiers.add(n.getTier());

        for (int tier : tiers) {
            emitRow(tip, host, def, lanes, tier, owned, pending, memSnapshot, false);
        }
        emitRow(tip, host, def, lanes, -1, owned, pending, memSnapshot, true);

        // After layout, compute edges from recorded centers.
        buildEdges(host, def, owned, pending);
    }

    private static void emitRow(TooltipMakerAPI tip, CharacterTabPanelPlugin host, TreeDef def,
                                List<String> lanes, int tier, Set<String> owned, Set<String> pending,
                                Map<String,Object> memSnapshot, boolean isCapstone) {
        for (int li = 0; li < lanes.size(); li++) {
            String lane = lanes.get(li);
            for (int slot = 0; slot <= 2; slot++) {
                Node match = findNode(def, lane, tier, slot, isCapstone);
                if (match == null) {
                    tip.addSpacer(NODE);
                    continue;
                }
                emitNodeButton(tip, host, match, owned, pending, memSnapshot);
            }
            if (!isCapstone && li < lanes.size() - 1) {
                Node hybrid = findHybrid(def, lanes.get(li), lanes.get(li + 1), tier);
                if (hybrid != null) {
                    emitNodeButton(tip, host, hybrid, owned, pending, memSnapshot);
                } else {
                    tip.addSpacer(HYBRID_COL_W);
                }
            }
        }
    }

    private static void emitNodeButton(TooltipMakerAPI tip, CharacterTabPanelPlugin host, Node n,
                                       Set<String> owned, Set<String> pending,
                                       Map<String,Object> memSnapshot) {
        boolean isOwned = owned.contains(n.getId());
        boolean isPending = pending.contains(n.getId());
        boolean isUnlockable = !isOwned && !isPending &&
                PrereqValidator.canPurchaseWithPending(
                        TreeRegistry.get(resolveTreeOf(n)),
                        memSnapshot, pending, n.getId());

        String label = (n.isCapstone() ? "\u2605 " : "") + truncate(n.getName(), 7);
        tip.addButton(label, n.getId(), NODE, NODE, 0f);

        com.fs.starfarer.api.ui.UIComponentAPI btn = tip.getPrev();
        if (btn != null) {
            float cx = btn.getPosition().getX() + NODE / 2f;
            float cy = btn.getPosition().getY() + NODE / 2f;
            host.nodeCenters.put(n.getId(), new float[]{cx, cy});
        }

        final Node captured = n;
        final boolean cOwned = isOwned, cPending = isPending, cUnlockable = isUnlockable;
        tip.addTooltipTo(new com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator() {
            @Override public boolean isTooltipExpandable(Object o) { return false; }
            @Override public float getTooltipWidth(Object o) { return 260f; }
            @Override public void createTooltip(com.fs.starfarer.api.ui.TooltipMakerAPI tt, boolean expanded, Object o) {
                tt.addTitle(captured.getName());
                if (captured.getDesc() != null && !captured.getDesc().isEmpty()) tt.addPara(captured.getDesc(), 4f);
                tt.addPara("Cost: " + captured.getCost(), 4f);
                String state = cOwned ? "OWNED" : cPending ? "PENDING" : cUnlockable ? "AVAILABLE" : "LOCKED";
                tt.addPara("State: " + state, 4f);
                if (!captured.getPrereqs().isEmpty()) {
                    tt.addPara("Prereqs: " + String.join(", ", captured.getPrereqs()), 4f);
                }
            }
        }, btn, com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private static void buildEdges(CharacterTabPanelPlugin host, TreeDef def,
                                   Set<String> owned, Set<String> pending) {
        Color teal   = new Color(80, 200, 170);
        Color amber  = new Color(220, 180, 60);
        Color gray   = new Color(80, 90, 110);
        Color green  = new Color(110, 200, 120);

        for (Node n : def.getNodes()) {
            float[] cCenter = host.nodeCenters.get(n.getId());
            if (cCenter == null) continue;
            for (String prereqId : n.getPrereqs()) {
                float[] pCenter = host.nodeCenters.get(prereqId);
                if (pCenter == null) continue;
                boolean pOwned = owned.contains(prereqId);
                boolean cOwned = owned.contains(n.getId());
                boolean cPending = pending.contains(n.getId());
                boolean stippled = n.isMerge();

                Color col;
                if (stippled) {
                    col = green;
                } else if (pOwned && cOwned) {
                    col = teal;
                } else if (cPending) {
                    col = amber;
                } else {
                    col = gray;
                }
                host.edges.add(new ConnectionLineDrawer.Edge(
                        pCenter[0], pCenter[1], cCenter[0], cCenter[1], col, stippled));
            }
        }
    }

    private static Node findNode(TreeDef def, String lane, int tier, int slot, boolean isCapstone) {
        for (Node n : def.getNodes()) {
            if (n.isMerge()) continue;
            if (isCapstone) {
                if (!n.isCapstone() || !n.getLane().equals(lane)) continue;
                if (n.getSlot() != slot) continue;
                return n;
            } else {
                if (n.isCapstone()) continue;
                if (!n.getLane().equals(lane) || n.getTier() != tier || n.getSlot() != slot) continue;
                return n;
            }
        }
        return null;
    }

    private static Node findHybrid(TreeDef def, String laneA, String laneB, int tier) {
        List<String> sorted = new ArrayList<String>(Arrays.asList(laneA, laneB));
        Collections.sort(sorted);
        String wantLane = "merge:" + sorted.get(0) + "_" + sorted.get(1);
        for (Node n : def.getMergeNodes()) {
            if (n.getLane().equals(wantLane) && n.getTier() == tier) return n;
        }
        return null;
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    /**
     * Looks up which Tree owns this node by searching all trees.
     */
    private static Tree resolveTreeOf(Node n) {
        for (Tree t : Tree.values()) {
            if (TreeRegistry.get(t).getNode(n.getId()) != null) return t;
        }
        return Tree.PILOT;
    }
}
