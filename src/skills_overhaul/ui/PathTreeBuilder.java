package skills_overhaul.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;

import java.awt.Color;
import java.util.*;

/**
 * Borderlands / Crusader Kings style skill-tree layout.
 *
 * Each tree has 3 "lanes" — rendered as 3 tall, side-by-side path columns.
 * Within one column:
 *   tier 0              centered root                (1 node)
 *   tier 1..4           three slot options per row   (0,1,2)
 *   tier 5 (capstone)   centered capstone            (1 node)
 *
 * Contentwidth (unscaled) will typically exceed the vanilla character slot;
 * the calling plugin is expected to apply a horizontal scroll offset.
 */
public final class PathTreeBuilder {

    // --- Visual metrics (mutable; set by configureForViewport) --------------
    // Baseline defaults below match the pre-scaling layout; call
    // {@link #configureForViewport} to retune for a given viewport width.
    public static float NODE        = 48f;
    public static float SLOT_GAP_X  = 8f;
    public static float TIER_GAP_Y  = 18f;
    public static float LANE_GAP_X  = 22f;
    public static float HEADER_H    = 30f;
    public static float LANE_INNER_PAD = 10f;

    // Derived widths — recomputed whenever metrics are reconfigured.
    public static float LANE_CONTENT_W = 5f * NODE + 4f * SLOT_GAP_X;
    public static float LANE_W = LANE_CONTENT_W + 2f * LANE_INNER_PAD;
    // Extra top padding inside a lane column before the first node row.
    // Raises the tier-0 slot away from the header so it isn't clipped.
    public static float LANE_TOP_PAD = 8f;

    // Colors shared with the rest of the UI.
    public static final Color COLOR_OWNED     = new Color( 80, 200, 140);
    public static final Color COLOR_PENDING   = new Color(230, 180,  70);
    public static final Color COLOR_UNLOCKABLE= new Color( 70, 150, 220);
    public static final Color COLOR_LOCKED    = new Color( 80,  90, 105);
    public static final Color COLOR_CAPSTONE  = new Color(220, 140, 220);

    private PathTreeBuilder() {}

    /**
     * Reset mutable layout fields to a consistent baseline before
     * {@link #configureForViewport} recomputes them (avoids stale values from
     * another tree or attach).
     */
    public static void resetLayoutDefaults() {
        NODE = 64f;
        SLOT_GAP_X  = clamp(NODE * 0.18f, 6f, 22f);
        LANE_GAP_X  = clamp(NODE * 0.40f, 16f, 52f);
        LANE_INNER_PAD = clamp(NODE * 0.20f, 8f, 28f);
        LANE_CONTENT_W = 5f * NODE + 4f * SLOT_GAP_X;
        LANE_W = LANE_CONTENT_W + 2f * LANE_INNER_PAD;
        TIER_GAP_Y   = clamp(NODE * 0.30f, 12f, 22f);
        HEADER_H     = clamp(NODE * 0.45f, 22f, 32f);
        LANE_TOP_PAD = clamp(NODE * 0.14f, 4f, 10f);
    }

    private static float totalContentHeight(int rowCount) {
        float bodyH = rowCount * NODE + Math.max(0, rowCount - 1) * TIER_GAP_Y;
        return HEADER_H + LANE_TOP_PAD + bodyH + 8f;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // --- Metrics ------------------------------------------------------------

    public static final class Metrics {
        public final float totalW, totalH;
        public final int rowCount;     // tiers including capstone row
        public final int maxTier;
        /**
         * In single-tree mode: lane count of the tree.
         * In multi-tree mode (buildPaths(List<TreeDef>) return): tree count.
         * Use {@link #treeCount} / {@link #isMultiTree} for unambiguous reads.
         */
        public final int laneCount;
        public final int treeCount;
        public Metrics(float totalW, float totalH, int rowCount, int maxTier, int laneCount) {
            this(totalW, totalH, rowCount, maxTier, laneCount, 1);
        }
        public Metrics(float totalW, float totalH, int rowCount, int maxTier,
                       int laneCount, int treeCount) {
            this.totalW = totalW; this.totalH = totalH;
            this.rowCount = rowCount; this.maxTier = maxTier;
            this.laneCount = laneCount; this.treeCount = treeCount;
        }
        public boolean isMultiTree() { return treeCount > 1; }
    }

    /** Multi-tree overload: sizes the layout to fit all trees side by side. */
    public static void configureForViewport(List<TreeDef> defs, float viewportW, float viewportH) {
        resetLayoutDefaults();
        int numTrees = Math.max(1, defs.size());
        int maxTier = 0;
        for (TreeDef d : defs) for (Node n : d.getNodes()) if (n.getTier() > maxTier) maxTier = n.getTier();
        int rowCount = maxTier + 1;

        float usableW = Math.max(200f, viewportW - 8f);
        float usableH = Math.max(200f, viewportH - 8f);

        // factorW per tree = 6.12n (5 slots + 4*slotGap + 2*innerPad)
        // factorW per tree gap = 0.80n (2 * laneGap ratio 0.40)
        float factorW = 6.12f * numTrees + 0.80f * Math.max(0, numTrees - 1);
        if (factorW < 1f) factorW = 1f;
        float nodeFromW = usableW / factorW;

        float factorH = 0.45f + 0.14f + rowCount + Math.max(0, rowCount - 1) * 0.30f;
        if (factorH < 1f) factorH = 1f;
        float nodeFromH = (usableH - 24f) / factorH;

        float node = clamp(Math.min(nodeFromW, nodeFromH), 44f, 120f);
        float slotGap  = clamp(node * 0.18f, 6f, 22f);
        float laneGap  = clamp(node * 0.40f, 16f, 52f);
        float innerPad = clamp(node * 0.20f, 8f, 28f);

        float laneW = 5f * node + 4f * slotGap + 2f * innerPad;
        float treeGap = 2f * laneGap;
        float total = numTrees * laneW + Math.max(0, numTrees - 1) * treeGap;
        if (total > usableW + 0.5f) {
            node = clamp(node * (usableW / total), 40f, 120f);
            slotGap  = clamp(node * 0.18f, 6f, 22f);
            laneGap  = clamp(node * 0.40f, 16f, 52f);
            innerPad = clamp(node * 0.20f, 8f, 28f);
        }

        NODE = node; SLOT_GAP_X = slotGap; LANE_GAP_X = laneGap;
        LANE_INNER_PAD = innerPad;
        LANE_CONTENT_W = 5f * NODE + 4f * SLOT_GAP_X;
        LANE_W = LANE_CONTENT_W + 2f * LANE_INNER_PAD;
        TIER_GAP_Y   = clamp(NODE * 0.30f, 12f, 22f);
        HEADER_H     = clamp(NODE * 0.45f, 22f, 32f);
        LANE_TOP_PAD = clamp(NODE * 0.14f, 4f, 10f);

        float bodyH = rowCount * NODE + Math.max(0, rowCount - 1) * TIER_GAP_Y;
        float totalH = HEADER_H + LANE_TOP_PAD + bodyH + 8f;
        if (totalH > usableH + 0.5f) {
            node = clamp(node * (usableH / totalH), 40f, 120f);
            slotGap  = clamp(node * 0.18f, 6f, 22f);
            laneGap  = clamp(node * 0.40f, 16f, 52f);
            innerPad = clamp(node * 0.20f, 8f, 28f);
            NODE = node; SLOT_GAP_X = slotGap; LANE_GAP_X = laneGap;
            LANE_INNER_PAD = innerPad;
            LANE_CONTENT_W = 5f * NODE + 4f * SLOT_GAP_X;
            LANE_W = LANE_CONTENT_W + 2f * LANE_INNER_PAD;
            TIER_GAP_Y   = clamp(NODE * 0.30f, 12f, 22f);
            HEADER_H     = clamp(NODE * 0.45f, 22f, 32f);
            LANE_TOP_PAD = clamp(NODE * 0.14f, 4f, 10f);
        }

        // Fine-grained fit loop.
        final float marginW = 2f, marginH = 2f;
        for (int iter = 0; iter < 16; iter++) {
            float tw = numTrees * LANE_W + Math.max(0, numTrees - 1) * 2f * LANE_GAP_X;
            float th = totalContentHeight(rowCount);
            if (tw <= viewportW - marginW && th <= viewportH - marginH) break;
            float scale = Math.min((viewportW - marginW) / Math.max(tw, 1f),
                    (viewportH - marginH) / Math.max(th, 1f));
            node = clamp(NODE * scale, 36f, 120f);
            slotGap  = clamp(node * 0.18f, 6f, 22f);
            laneGap  = clamp(node * 0.40f, 16f, 52f);
            innerPad = clamp(node * 0.20f, 8f, 28f);
            NODE = node; SLOT_GAP_X = slotGap; LANE_GAP_X = laneGap;
            LANE_INNER_PAD = innerPad;
            LANE_CONTENT_W = 5f * NODE + 4f * SLOT_GAP_X;
            LANE_W = LANE_CONTENT_W + 2f * LANE_INNER_PAD;
            TIER_GAP_Y   = clamp(NODE * 0.30f, 12f, 22f);
            HEADER_H     = clamp(NODE * 0.45f, 22f, 32f);
            LANE_TOP_PAD = clamp(NODE * 0.14f, 4f, 10f);
        }
    }

    /** Derive lane count from distinct node lane values. */
    private static int deriveLaneCount(TreeDef def) {
        Set<String> lanes = new LinkedHashSet<String>();
        for (Node n : def.getNodes()) {
            lanes.add(n.getLane());
        }
        return Math.max(1, lanes.size());
    }

    /** Derive ordered lane name list from distinct node lane values (insertion order). */
    private static List<String> deriveLanes(TreeDef def) {
        List<String> lanes = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Node n : def.getNodes()) {
            if (seen.add(n.getLane())) lanes.add(n.getLane());
        }
        if (lanes.isEmpty()) lanes.add("default");
        return lanes;
    }

    public static Metrics computeMetrics(TreeDef def) {
        int maxTier = 0;
        for (Node n : def.getNodes()) if (n.getTier() > maxTier) maxTier = n.getTier();
        int rowCount = maxTier + 1;
        int laneCount = deriveLaneCount(def);

        float bodyH = rowCount * NODE + Math.max(0, rowCount - 1) * TIER_GAP_Y;
        float totalH = HEADER_H + LANE_TOP_PAD + bodyH + 8f;

        float totalW = 0f;
        for (int i = 0; i < laneCount; i++) {
            totalW += LANE_W;
            if (i < laneCount - 1) totalW += 2f * LANE_GAP_X;
        }
        return new Metrics(totalW, totalH, rowCount, maxTier, laneCount);
    }

    // --- Main build ---------------------------------------------------------

    /**
     * Lay out the tree into the given canvas. All components added are tracked
     * in {@code tracker} so the caller can remove them on rebuild.
     *
     * @param scrollX horizontal scroll offset in canvas pixels (>= 0).
     *                GL scissor in the host clips content; all lanes are emitted.
     */
    public static Metrics buildPaths(CustomPanelAPI panel, CharacterTabPanelPlugin host, Tree t,
                                     float originX, float originY, float viewportW, float viewportH,
                                     float scrollX,
                                     List<UIComponentAPI> tracker) {
        List<TreeDef> defs = TreeRegistry.getTrees(t);
        float treeGapX = 2f * LANE_GAP_X;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Set<String> owned = new HashSet<String>();
        Map<String, Object> memSnapshot = new HashMap<String, Object>();
        for (TreeDef def : defs) {
            for (Node n : def.getNodes()) {
                String k = SkillMemory.purchasedKey(n.getId());
                if (mem.contains(k) && mem.getBoolean(k)) {
                    owned.add(n.getId());
                    memSnapshot.put(k, Boolean.TRUE);
                }
            }
        }
        Set<String> pending = host.pendingByTree.get(t);

        // Compute combined content dimensions.
        float contentW = 0f;
        float contentH = 0f;
        for (int di = 0; di < defs.size(); di++) {
            Metrics m = computeMetrics(defs.get(di));
            if (di > 0) contentW += treeGapX;
            contentW += m.totalW;
            contentH = Math.max(contentH, m.totalH);
        }

        float bodyY0 = originY + Math.max(0f, (viewportH - contentH) * 0.5f);

        float treeCursor = 0f;
        for (int di = 0; di < defs.size(); di++) {
            TreeDef def = defs.get(di);
            Metrics metrics = computeMetrics(def);
            if (di > 0) treeCursor += treeGapX;

            List<String> mainLanes = deriveLanes(def);
            float[] laneX = new float[mainLanes.size()];
            float laneCursor = treeCursor;
            for (int i = 0; i < mainLanes.size(); i++) {
                laneX[i] = laneCursor;
                laneCursor += LANE_W;
                if (i < mainLanes.size() - 1) laneCursor += 2f * LANE_GAP_X;
            }
            treeCursor = laneCursor;

            for (int li = 0; li < mainLanes.size(); li++) {
                String lane = mainLanes.get(li);
                float colXContent = laneX[li];
                float colXViewport = originX + (colXContent - scrollX);

                emitLaneColumn(panel, host, def, lane, metrics,
                        colXViewport, bodyY0, owned, pending, memSnapshot, tracker);
            }

            syncNodeGeometryFromResolvedPositions(panel, host, def);
        }
        host.edges.clear();
        for (TreeDef def : defs) buildEdges(host, def, owned, pending);

        // Empty tab (e.g. empty *_index.json): defs.get(0) would throw inside a UI callback.
        if (defs.isEmpty()) return new Metrics(0f, 0f, 0, 0, 0, 0);
        Metrics first = computeMetrics(defs.get(0));
        // Multi-tree return: laneCount preserved for first tree; treeCount = defs.size().
        return new Metrics(contentW, contentH, first.rowCount, first.maxTier,
                first.laneCount, defs.size());
    }

    /**
     * Copies {@link CharacterTabPanelPlugin#nodeLocal} / {@code nodeCenters} from
     * resolved {@link PositionAPI} on the panel, then rebuilds edges. The layout
     * math in {@link #emitNode} matches {@code inTL} in theory, but the engine
     * commits slightly different bounds; GL + {@code SpriteAPI} must use the same
     * numbers as {@link PositionAPI#containsEvent} or icons drift from hit rects.
     */
    public static void refreshEdgesAfterLayout(CustomPanelAPI panel, CharacterTabPanelPlugin host, Tree tree,
                                              TreeDef def, Set<String> owned, Set<String> pending) {
        syncNodeGeometryFromResolvedPositions(panel, host, def);
        host.edges.clear();
        buildEdges(host, def, owned, pending);
    }

    /**
     * Re-snapshots purchased nodes from memory and refreshes geometry (call from
     * {@link CharacterTabPanelPlugin#renderBelow} so GL tracks layout after the
     * engine commits {@link PositionAPI} bounds).
     */
    public static void refreshEdgesAfterLayout(CustomPanelAPI panel, CharacterTabPanelPlugin host) {
        Tree tree = host.currentTree;
        List<TreeDef> defs = TreeRegistry.getTrees(tree);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Set<String> owned = new HashSet<String>();
        for (TreeDef def : defs) {
            for (Node n : def.getNodes()) {
                String k = SkillMemory.purchasedKey(n.getId());
                if (mem.contains(k) && mem.getBoolean(k)) owned.add(n.getId());
            }
        }
        Set<String> pending = host.pendingByTree.get(tree);
        for (TreeDef def : defs) syncNodeGeometryFromResolvedPositions(panel, host, def);
        host.edges.clear();
        for (TreeDef def : defs) buildEdges(host, def, owned, pending);
    }

    private static void syncNodeGeometryFromResolvedPositions(CustomPanelAPI panel,
                                                              CharacterTabPanelPlugin host,
                                                              TreeDef def) {
        float px0 = panel.getPosition().getX();
        float py0 = panel.getPosition().getY();
        float n = NODE;
        for (Node node : def.getNodes()) {
            PositionAPI p = host.nodePositions.get(node.getId());
            if (p == null) continue;
            float cx = p.getCenterX() - px0;
            float cy = p.getCenterY() - py0;
            host.nodeCenters.put(node.getId(), new float[]{ cx, cy });
            host.nodeLocal.put(node.getId(), new float[]{ cx - n * 0.5f, cy - n * 0.5f });
        }
    }

    // --- Column emission ----------------------------------------------------

    private static void emitLaneColumn(CustomPanelAPI panel, CharacterTabPanelPlugin host,
                                       TreeDef def, String lane, Metrics metrics,
                                       float colX, float contentYTop,
                                       Set<String> owned, Set<String> pending,
                                       Map<String, Object> memSnapshot,
                                       List<UIComponentAPI> tracker) {
        // Header band with lane name. (R1 fix: pass lane key, not tree key —
        // lane key may be namespaced "tree{N}:..." in multi-tree path; strip
        // prefix in prettyLaneName.)
        TooltipMakerAPI header = panel.createUIElement(LANE_W, HEADER_H, false);
        String label = prettyLaneName(lane);
        Color laneAccent = new Color(150, 200, 240);
        try {
            header.setParaFontVictor14();
        } catch (Throwable ignored) {}
        header.addPara(label, laneAccent, 0f);
        try {
            header.setParaFontDefault();
        } catch (Throwable ignored) {}
        PositionAPI hpos = panel.addUIElement(header);
        if (hpos != null) hpos.inTL(colX, contentYTop);
        tracker.add(header);

        float bodyY = contentYTop + HEADER_H + LANE_TOP_PAD;

        // Per-lane max tier (main lane only). Using the global tree maxTier here made short
        // lanes repeat the capstone row at the global bottom tier index, stacking duplicate
        // hit rects on top of the real root (e.g. "Basic Gunnery" clickable at top and bottom).
        int laneMaxTier = 0;
        for (Node n : def.getNodes()) {
            if (!lane.equals(n.getLane())) continue;
            if (n.getTier() > laneMaxTier) laneMaxTier = n.getTier();
        }

        // Emit nodes tier by tier.
        for (int tier = 0; tier <= laneMaxTier; tier++) {
            float rowY = bodyY + tier * (NODE + TIER_GAP_Y);
            boolean isCapstone = (tier == laneMaxTier);

            if (tier == 0 || isCapstone) {
                // Single centered node for root and capstone.
                Node n = findCenteredNode(def, lane, tier, isCapstone);
                // Sole row (laneMaxTier==0): isCapstone is true, so try the non-capstone root lookup too.
                if (n == null && laneMaxTier == 0) {
                    n = findCenteredNode(def, lane, 0, false);
                    if (n == null) n = findCenteredNode(def, lane, 0, true);
                }
                if (n != null) {
                    float nx = colX + LANE_INNER_PAD + (LANE_CONTENT_W - NODE) / 2f;
                    emitNode(panel, host, n, owned, pending, memSnapshot, nx, rowY, tracker);
                }
            } else {
                for (int slot = 0; slot < 5; slot++) {
                    Node n = findNode(def, lane, tier, slot);
                    if (n == null) continue;
                    float nx = colX + LANE_INNER_PAD + slot * (NODE + SLOT_GAP_X);
                    emitNode(panel, host, n, owned, pending, memSnapshot, nx, rowY, tracker);
                }
            }
        }
    }

    // --- Node emission ------------------------------------------------------

    private static void emitNode(CustomPanelAPI panel, CharacterTabPanelPlugin host,
                                 Node n, Set<String> owned, Set<String> pending,
                                 Map<String, Object> memSnapshot,
                                 float x, float y,
                                 List<UIComponentAPI> tracker) {
        boolean isOwned      = owned.contains(n.getId());
        boolean isPending    = pending.contains(n.getId());
        boolean isUnlockable = !isOwned && !isPending &&
                PrereqValidator.canPurchaseWithPending(
                        TreeRegistry.findTreeOfAnywhere(n.getId()), memSnapshot, pending, n.getId());

        Color borderColor;
        if (n.isCapstone()) {
            borderColor = isOwned ? COLOR_CAPSTONE :
                          isPending ? COLOR_PENDING :
                          isUnlockable ? COLOR_UNLOCKABLE : COLOR_LOCKED;
        } else {
            if (isOwned) borderColor = COLOR_OWNED;
            else if (isPending) borderColor = COLOR_PENDING;
            else if (isUnlockable) borderColor = COLOR_UNLOCKABLE;
            else borderColor = COLOR_LOCKED;
        }

        // Empty TooltipMaker hit rect — MUST use addUIElement like lane headers
        // so inTL() shares the same top-down Y convention. createCustom +
        // addComponent used bottom-up Y and pushed icons (GL) away from labels
        // (UI) by hundreds of pixels. Visuals stay in renderBelow; no paras here.
        //
        // If the same node id is ever emitted twice (rebuild race, bad data), remove the
        // previous hit child so only one invisible rect receives input (fixes duplicate
        // triggers for one skill).
        UIComponentAPI stale = host.nodeHitById.remove(n.getId());
        if (stale != null) {
            try { panel.removeComponent(stale); } catch (Throwable ignored) {}
            tracker.remove(stale);
        }
        PositionAPI nodePos = null;
        try {
            TooltipMakerAPI hit = panel.createUIElement(NODE, NODE, false);
            nodePos = panel.addUIElement(hit);
            if (nodePos != null) nodePos.inTL(x, y);
            tracker.add(hit);
            host.nodeHitById.put(n.getId(), hit);
        } catch (Throwable e) {
            Global.getLogger(PathTreeBuilder.class).warn(
                    "Failed to build hit rect for: " + n.getId(), e);
        }

        // Bottom-up panel-local geometry (same space as syncNodeGeometryFromResolvedPositions).
        float localY = PanelCoords.localYFromTop(panel.getPosition().getHeight(), y, NODE);
        host.nodeCenters.put(n.getId(), new float[]{x + NODE / 2f, localY + NODE / 2f});
        host.nodeLocal.put(n.getId(), new float[]{x, localY});
        if (nodePos != null) host.nodePositions.put(n.getId(), nodePos);
        host.nodeStates.put(n.getId(),
                new CharacterTabPanelPlugin.NodeState(n, isOwned, isPending, isUnlockable));
        host.nodeBorderColors.put(n.getId(), borderColor);
    }

    // --- Edges --------------------------------------------------------------

    private static void buildEdges(CharacterTabPanelPlugin host, TreeDef def,
                                   Set<String> owned, Set<String> pending) {
        Color teal  = new Color( 90, 210, 180);
        Color amber = new Color(230, 180,  70);
        Color blue  = new Color( 80, 150, 220);
        Color gray  = new Color( 70,  85, 105);
        // Hoisted: invariant per call, and this runs every frame from renderBelow.
        Map<String, Object> ownedPendingSnap = snapshotOwnedPending(owned, pending);

        for (Node n : def.getNodes()) {
            float[] cCenter = host.nodeCenters.get(n.getId());
            if (cCenter == null) continue;
            for (String prereqId : n.getPrereqs()) {
                float[] pCenter = host.nodeCenters.get(prereqId);
                if (pCenter == null) continue;
                boolean pOwned = owned.contains(prereqId);
                boolean cOwned = owned.contains(n.getId());
                boolean cPending = pending.contains(n.getId());
                boolean prereqSatisfied = pOwned || pending.contains(prereqId);
                boolean childReady = PrereqValidator.canPurchaseWithPending(
                        def, ownedPendingSnap, pending, n.getId());

                Color col;
                if (pOwned && cOwned) {
                    col = teal;
                } else if (cPending && prereqSatisfied) {
                    col = amber;
                } else if (childReady && prereqSatisfied) {
                    col = blue;
                } else {
                    col = gray;
                }

                host.edges.add(new ConnectionLineDrawer.Edge(
                        pCenter[0], pCenter[1], cCenter[0], cCenter[1],
                        col, false, prereqSatisfied));
            }
        }
    }

    private static Map<String, Object> snapshotOwnedPending(Set<String> owned, Set<String> pending) {
        Map<String, Object> snap = new HashMap<String, Object>();
        for (String id : owned)   snap.put(SkillMemory.purchasedKey(id), Boolean.TRUE);
        for (String id : pending) snap.put(SkillMemory.purchasedKey(id), Boolean.TRUE);
        return snap;
    }

    // --- Lookup helpers -----------------------------------------------------

    private static Node findNode(TreeDef def, String lane, int tier, int slot) {
        for (Node n : def.getNodes()) {
            if (n.isCapstone()) continue;
            if (!lane.equals(n.getLane())) continue;
            if (n.getTier() != tier) continue;
            if (n.getSlot() != slot) continue;
            return n;
        }
        return null;
    }

    /** Returns the single root (tier 0) or capstone node on a main lane. */
    private static Node findCenteredNode(TreeDef def, String lane, int tier, boolean isCapstone) {
        for (Node n : def.getNodes()) {
            if (!lane.equals(n.getLane())) continue;
            if (isCapstone) {
                if (!n.isCapstone()) continue;
                return n;
            } else {
                if (n.isCapstone()) continue;
                if (n.getTier() != tier) continue;
                return n;
            }
        }
        return null;
    }

    private static String prettyLaneName(String lane) {
        if (lane == null || lane.isEmpty()) return "";
        // Strip multi-tree prefix "tree{N}:" if present.
        int colon = lane.indexOf(':');
        String s = colon >= 0 ? lane.substring(colon + 1) : lane;
        // Strip leading tab prefix (e.g. "pilot_defense" → "defense").
        int underscore = s.indexOf('_');
        String label = underscore >= 0 ? s.substring(underscore + 1) : s;
        return label.replace('_', ' ').toUpperCase();
    }
}
