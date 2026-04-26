package skills_overhaul.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import skills_overhaul.level.XPCurve;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;

import java.awt.Color;
import java.util.*;

/**
 * Custom Character tab — Borderlands / CK-style path tree.
 *
 *        [PILOT 1] [COMMANDER 2] [ADMIN 3]   <- outside top
 *   +----------------------------------------------------+
 *   | left rail |     path viewport                      |
 *   |           |  [ LANE_A ][ BRIDGE ][ LANE_B ][ ... ]  |
 *   |           |                                         |
 *   |           |                                         |
 *   +----------------------------------------------------+
 *   |                bottom bar (XP / points)             |
 *
 * Lane columns auto-scale to fit the available viewport width without
 * horizontal scrolling whenever possible.
 */
public final class CharacterTabPanelPlugin extends BaseCustomUIPanelPlugin {

    /** Bumped when coordinate / layout semantics change (check logs after deploy). */
    public static final String UI_VERSION = "V17-EVENT-HIT-FIRST";

    static final float TAB_BAR_H    = 24f;
    static final float TAB_W        = 110f;
    static final float TAB_GAP      = 4f;
    static final float LEFT_RAIL_W  = 240f;
    static final float BOTTOM_BAR_H = 44f;
    static final float PAD          = 14f;
    static final float TOP_GAP      = 4f;   // small gap between tab bar and window
    static final float SCROLL_H     = 10f;

    private CustomPanelAPI panel;

    // --- Tree state ---------------------------------------------------------
    Tree currentTree = Tree.PILOT;
    final Map<Tree, Set<String>> pendingByTree = new EnumMap<Tree, Set<String>>(Tree.class);
    final Map<Tree, Float> scrollXByTree = new EnumMap<Tree, Float>(Tree.class);

    // --- Layout output (consumed by builder + input + rendering) -----------
    final List<ConnectionLineDrawer.Edge> edges = new ArrayList<ConnectionLineDrawer.Edge>();
    final Map<String, float[]> nodeCenters = new HashMap<String, float[]>();
    /** Panel-local top-left (x, y) for each node icon; used for GL rendering. */
    final Map<String, float[]> nodeLocal = new HashMap<String, float[]>();
    final Map<String, PositionAPI> nodePositions = new HashMap<String, PositionAPI>();
    /** Invisible hit TooltipMakers — one per node id; used to strip stale rects on re-emit. */
    final Map<String, UIComponentAPI> nodeHitById = new HashMap<String, UIComponentAPI>();
    final Map<String, Color> nodeBorderColors = new HashMap<String, Color>();
    final Map<String, NodeState> nodeStates = new HashMap<String, NodeState>();
    private final List<PathTreeBuilder.LaneColumn> laneColumns = new ArrayList<PathTreeBuilder.LaneColumn>();
    private final List<UIComponentAPI> uiChildren = new ArrayList<UIComponentAPI>();
    /** Left rail element — used to snap GL portrait frame to committed layout. */
    private TooltipMakerAPI leftRailTip;

    // Viewport rect in panel-local (top-down y) coords.
    private float viewX, viewY, viewW, viewH;
    // Portrait rect in panel-local (top-down y) coords for border drawing.
    private float portraitX, portraitY, portraitW, portraitH;
    private float contentW;
    /** Panel-local top-down Y where the bottom TooltipMaker bar starts (see rebuild). */
    private float bottomBarTopY;

    // --- Hover / tooltip state ----------------------------------------------
    private String hoveredNodeId;
    private TooltipMakerAPI tooltipElement;
    private float tooltipLocalX, tooltipLocalY, tooltipLocalW, tooltipLocalH;
    /** Latest cursor in panel-local coords (updated from mouse events in {@link #processInput}). */
    private float lastPanelMouseX, lastPanelMouseY;

    public static final class NodeState {
        public final Node node;
        public final boolean owned, pending, unlockable;
        public NodeState(Node node, boolean owned, boolean pending, boolean unlockable) {
            this.node = node;
            this.owned = owned;
            this.pending = pending;
            this.unlockable = unlockable;
        }
    }

    public CharacterTabPanelPlugin() {
        for (Tree t : Tree.values()) {
            pendingByTree.put(t, new LinkedHashSet<String>());
            scrollXByTree.put(t, 0f);
        }
    }

    public void attach(CustomPanelAPI panel) {
        this.panel = panel;
        try {
            java.security.CodeSource cs = CharacterTabPanelPlugin.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Global.getLogger(CharacterTabPanelPlugin.class).info(
                        "[SkillsOverhaul/UI] attach " + UI_VERSION
                        + " panel=" + panel.getPosition().getWidth() + "x" + panel.getPosition().getHeight()
                        + " jar=" + cs.getLocation());
            } else {
                Global.getLogger(CharacterTabPanelPlugin.class).info(
                        "[SkillsOverhaul/UI] attach " + UI_VERSION
                        + " panel=" + panel.getPosition().getWidth() + "x" + panel.getPosition().getHeight());
            }
        } catch (Throwable t) {
            Global.getLogger(CharacterTabPanelPlugin.class).info(
                    "[SkillsOverhaul/UI] attach " + UI_VERSION
                    + " panel=" + panel.getPosition().getWidth() + "x" + panel.getPosition().getHeight());
        }
        rebuild();
    }

    public void rebuild() {
        if (panel == null) return;
        Global.getLogger(CharacterTabPanelPlugin.class).info("[SkillsOverhaul/UI] rebuild " + UI_VERSION);

        for (UIComponentAPI child : uiChildren) {
            try { panel.removeComponent(child); } catch (Throwable ignored) {}
        }
        uiChildren.clear();
        edges.clear();
        nodeCenters.clear();
        nodeLocal.clear();
        nodePositions.clear();
        nodeHitById.clear();
        nodeBorderColors.clear();
        nodeStates.clear();
        laneColumns.clear();
        tooltipElement = null;
        hoveredNodeId = null;
        tooltipLocalX = -1f;
        leftRailTip = null;
        portraitW = 0f;
        portraitH = 0f;

        float w = panel.getPosition().getWidth();
        float h = panel.getPosition().getHeight();

        // Window area sits UNDER the tab strip. Tabs render at y=0 and are
        // visually "outside" the window frame (their bottoms meet winY).
        float winX = PAD;
        float winY = TAB_BAR_H + TOP_GAP;
        float winW = w - 2f * PAD;
        float winH = h - winY - PAD;

        // --- Tab strip (outside of window, smaller, active-tab bleeds into
        //     top of window frame for a vanilla-like tabbed look) ---------
        float tabBarW = Tree.values().length * (TAB_W + TAB_GAP);
        float tabX = (w - tabBarW) / 2f;
        float tabY = 0f;

        Color baseC = safeColor(new Color(140, 200, 240), true);
        Color bgC   = safeColor(new Color( 18,  35,  60), false);
        Color hotkeyC = new Color(255, 220, 70);
        int idx = 1;
        for (Tree t : Tree.values()) {
            TooltipMakerAPI tabBtn = panel.createUIElement(TAB_W, TAB_BAR_H, false);
            // Use tree key (uppercased) without bracketed hotkey. The hotkey
            // is overlaid separately in yellow below.
            String label = t.key.toUpperCase();
            ButtonAPI btn = tabBtn.addButton(label, "__tab__" + t.name(),
                    baseC, bgC, Alignment.MID, CutStyle.C2_MENU, TAB_W, TAB_BAR_H, 0f);
            if (btn != null && t == currentTree) btn.setChecked(true);
            PositionAPI pos = panel.addUIElement(tabBtn);
            if (pos != null) pos.inTL(tabX, tabY);
            uiChildren.add(tabBtn);

            // Yellow hotkey overlay: "[N]" text placed to the right of the
            // label inside the tab. We approximate placement since we can't
            // measure the button's text width directly.
            TooltipMakerAPI hot = panel.createUIElement(28f, 16f, false);
            hot.addPara("[%s]", 0f, hotkeyC, "" + idx);
            PositionAPI hpos = panel.addUIElement(hot);
            if (hpos != null) hpos.inTL(tabX + TAB_W - 26f, tabY + 4f);
            uiChildren.add(hot);

            tabX += TAB_W + TAB_GAP;
            idx++;
        }

        // --- Left rail -----------------------------------------------------
        float leftX = winX + PAD;
        float leftY = winY + PAD;
        float leftW = Math.min(LEFT_RAIL_W, Math.max(200f, winW * 0.22f));
        float leftH = winH - 2f * PAD - BOTTOM_BAR_H;

        TooltipMakerAPI left = panel.createUIElement(leftW, leftH, false);
        paintLeftRail(left, leftW, leftX, leftY);
        PositionAPI leftPos = panel.addUIElement(left);
        if (leftPos != null) leftPos.inTL(leftX, leftY);
        uiChildren.add(left);
        leftRailTip = left;

        // --- Viewport ------------------------------------------------------
        viewX = leftX + leftW + PAD;
        viewY = leftY;
        viewW = (winX + winW) - viewX - PAD;
        viewH = leftH - SCROLL_H - 6f;

        // Configure builder metrics to fit the viewport for this tree.
        TreeDef def = TreeRegistry.get(currentTree);
        PathTreeBuilder.configureForViewport(def, viewW, viewH);

        PathTreeBuilder.Metrics metrics = PathTreeBuilder.computeMetrics(def);
        contentW = metrics.totalW;

        Global.getLogger(CharacterTabPanelPlugin.class).info(
                "[SkillsOverhaul/UI] layout " + currentTree.key
                + " view=" + (int) viewW + "x" + (int) viewH
                + " node=" + PathTreeBuilder.NODE
                + " laneW=" + PathTreeBuilder.LANE_W
                + " total=" + (int) metrics.totalW + "x" + (int) metrics.totalH);

        float maxScroll = Math.max(0f, contentW - viewW);
        float sx = scrollXByTree.get(currentTree);
        if (sx < 0f) sx = 0f;
        if (sx > maxScroll) sx = maxScroll;
        // When the tree fits, never keep a stale positive scroll (would clip the
        // right lanes after resolution / layout changes).
        if (contentW <= viewW + 1f) sx = 0f;
        scrollXByTree.put(currentTree, sx);

        PathTreeBuilder.buildPaths(panel, this, currentTree,
                viewX, viewY, viewW, viewH, sx, uiChildren, laneColumns);

        // --- Bottom bar ----------------------------------------------------
        TooltipMakerAPI bottom = panel.createUIElement(winW - 2f * PAD, BOTTOM_BAR_H, false);
        paintBottomBar(bottom);
        PositionAPI botPos = panel.addUIElement(bottom);
        if (botPos != null) botPos.inTL(winX + PAD, leftY + leftH + PAD);
        uiChildren.add(bottom);
        bottomBarTopY = leftY + leftH + PAD;
    }

    private static final float PORTRAIT_W = 112f;

    private int ownedCountForTree(Tree t) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int count = 0;
        try {
            TreeDef def = TreeRegistry.get(t);
            for (Node n : def.getNodes()) {
                String k = SkillMemory.purchasedKey(n.getId());
                if (mem.contains(k) && mem.getBoolean(k)) count++;
            }
        } catch (Throwable ignored) {}
        return count;
    }

    /** Rebuild the floating tooltip element for {@link #hoveredNodeId}. */
    private void updateTooltip() {
        if (panel == null) return;
        if (tooltipElement != null) {
            try { panel.removeComponent(tooltipElement); } catch (Throwable ignored) {}
            uiChildren.remove(tooltipElement);
            tooltipElement = null;
        }
        tooltipLocalX = -1f;
        if (hoveredNodeId == null) return;

        // processInput can run before renderBelow in a frame; snap nodeLocal from
        // committed hit-rect positions so tooltip inTL matches GL + containsEvent.
        if (!nodePositions.isEmpty()) {
            PathTreeBuilder.refreshEdgesAfterLayout(panel, this);
        }

        NodeState ns = nodeStates.get(hoveredNodeId);
        if (ns == null) return;

        Node n = ns.node;
        float tW = 280f;
        float tH = 120f;
        float[] tl = computeTooltipPanelTL(hoveredNodeId, tW, tH);
        if (tl == null) return;

        TooltipMakerAPI tt = panel.createUIElement(tW, tH, false);

        Color titleC = ns.owned ? PathTreeBuilder.COLOR_OWNED
                     : ns.pending ? PathTreeBuilder.COLOR_PENDING
                     : ns.unlockable ? PathTreeBuilder.COLOR_UNLOCKABLE
                     : new Color(200, 210, 230);
        tt.addPara(n.getName() == null ? n.getId() : n.getName(), titleC, 0f);

        String desc = n.getDesc();
        if (desc != null && !desc.isEmpty()) {
            tt.addPara(desc, new Color(200, 210, 225), 6f);
        }

        String statusLine;
        if (ns.owned) {
            statusLine = "Owned";
        } else if (ns.pending) {
            statusLine = "Pending  (cost " + n.getCost() + ")";
        } else if (ns.unlockable) {
            statusLine = "Unlockable  (cost " + n.getCost() + ")";
        } else {
            statusLine = "Locked  (cost " + n.getCost() + ")";
        }
        if (n.isCapstone()) statusLine += "  - Capstone";
        tt.addPara(statusLine, titleC, 6f);

        float tx = tl[0];
        float ty = tl[1];

        PositionAPI tp = panel.addUIElement(tt);
        if (tp != null) tp.inTL(tx, ty);
        tooltipElement = tt;
        tooltipLocalX = tx;
        tooltipLocalY = ty;
        tooltipLocalW = tW;
        tooltipLocalH = tH;
        uiChildren.add(tt);
    }

    /**
     * Panel-local top-left for the hover tooltip: X stays beside the node,
     * Y tracks {@link #lastPanelMouseY} (vertically centered on the cursor).
     */
    private float[] computeTooltipPanelTL(String nodeId, float tW, float tH) {
        if (panel == null || nodeId == null) return null;
        float panelW = panel.getPosition().getWidth();
        float panelH = panel.getPosition().getHeight();
        float nSize = PathTreeBuilder.NODE;
        float pIx = panel.getPosition().getX();
        float ax;
        PositionAPI hit = nodePositions.get(nodeId);
        if (hit != null) {
            float cx = hit.getCenterX() - pIx;
            ax = cx - nSize * 0.5f;
        } else {
            float[] xy = nodeLocal.get(nodeId);
            if (xy == null) return null;
            ax = xy[0];
        }
        float tx = ax + nSize + 8f;
        if (tx + tW > panelW - 8f) tx = Math.max(8f, ax - tW - 8f);
        float ty = lastPanelMouseY - tH * 0.5f;
        if (ty < 8f) ty = 8f;
        if (ty + tH > panelH - 8f) ty = Math.max(8f, panelH - tH - 8f);
        return new float[]{tx, ty};
    }

    /** Move an existing tooltip with the cursor without rebuilding its contents. */
    private void repositionTooltip() {
        if (panel == null || tooltipElement == null || hoveredNodeId == null) return;
        float[] tl = computeTooltipPanelTL(hoveredNodeId, tooltipLocalW, tooltipLocalH);
        if (tl == null) return;
        try {
            if (tooltipElement instanceof UIComponentAPI) {
                PositionAPI pos = ((UIComponentAPI) tooltipElement).getPosition();
                if (pos != null) pos.inTL(tl[0], tl[1]);
            }
        } catch (Throwable ignored) {}
        tooltipLocalX = tl[0];
        tooltipLocalY = tl[1];
    }

    private Color safeColor(Color fallback, boolean base) {
        try {
            return base ? Misc.getBasePlayerColor() : Misc.getDarkPlayerColor();
        } catch (Throwable t) {
            return fallback;
        }
    }

    private void paintLeftRail(TooltipMakerAPI tip, float railW, float leftX, float leftY) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        portraitW = 0f;
        portraitH = 0f;

        com.fs.starfarer.api.characters.PersonAPI player = Global.getSector().getPlayerPerson();
        if (player != null) {
            String portrait = player.getPortraitSprite();
            if (portrait != null && !portrait.isEmpty()) {
                try { tip.addImage(portrait, PORTRAIT_W, 0f); } catch (Throwable ignored) {}
            }
            // Cache portrait rect so renderBelow can draw a border around it.
            portraitX = leftX + (railW - PORTRAIT_W) / 2f;
            portraitY = leftY;
            portraitW = PORTRAIT_W;
            portraitH = PORTRAIT_W; // portraits are square in vanilla
            tip.addPara(player.getNameString(), 6f);
        }
        tip.addPara("", 2f);

        int sp = Global.getSector().getPlayerStats().getStoryPoints();
        tip.addPara("Story points: " + sp, 4f);

        float freeUni = mem.contains(SkillMemory.freeUniKey())
                ? mem.getFloat(SkillMemory.freeUniKey()) : 0f;
        float migPool = mem.contains("$skills.points.universal_migrated_unallocated")
                ? mem.getFloat("$skills.points.universal_migrated_unallocated") : 0f;
        tip.addPara("Free Uni: " + (int) freeUni, 6f);
        tip.addPara("Mig Pool: " + (int) migPool, 2f);

        tip.addPara("", 6f);
        tip.addPara("Keys 1-3 switch tabs", new Color(150, 160, 180), 2f);

        // Action buttons last (vanilla-style Alignment + CutStyle) so later widgets never
        // stack fake hit targets on top of Respec / Confirm.
        tip.addPara("", 8f);
        float btnW = Math.max(120f, railW - 10f);
        float btnH = 28f;
        Set<String> pending = pendingByTree.get(currentTree);
        int ownedInTree = ownedCountForTree(currentTree);
        if (pending.isEmpty()) {
            if (ownedInTree > 0) {
                tip.addButton("Respec (1 SP)", "__respec__",
                        new Color(180, 60, 60), new Color(100, 30, 30),
                        Alignment.MID, CutStyle.C2_MENU, btnW, btnH, 4f);
            } else {
                ButtonAPI b = tip.addButton("Respec (n/a)", null,
                        new Color(90, 90, 100), new Color(30, 30, 40),
                        Alignment.MID, CutStyle.C2_MENU, btnW, btnH, 4f);
                if (b != null) try { b.setEnabled(false); } catch (Throwable ignored) {}
            }
        } else {
            int costSum = sumPendingCost(pending);
            tip.addButton("Confirm (" + costSum + " pts)", "__confirm__",
                    new Color(60, 160, 90), new Color(30, 90, 50),
                    Alignment.MID, CutStyle.C2_MENU, btnW, btnH, 4f);
        }
    }

    // Cached XP values computed once per rebuild, used by renderBelow for the bar.
    private int  bbLevel;
    private int  bbPoints;
    private long bbCur;
    private long bbReq;

    private void paintBottomBar(TooltipMakerAPI tip) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        Map<String, Object> m = new HashMap<String, Object>();
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
        if (req <= 0) req = 1;

        bbLevel  = level;
        bbPoints = pts;
        bbCur    = cur;
        bbReq    = req;

        String text = currentTree.key.toUpperCase()
                + "   Level " + level
                + "   " + cur + " / " + req + " XP"
                + "   (" + pts + " points)"
                + "            " + UI_VERSION;
        tip.addPara(text, 2f);
    }

    // ------------------------------------------------------------ rendering

    @Override
    public void renderBelow(float alphaMult) {
        if (panel == null) return;
        // Align GL/nodeLocal with committed UI positions (hit boxes use PositionAPI).
        if (!nodePositions.isEmpty()) {
            PathTreeBuilder.refreshEdgesAfterLayout(panel, this);
        }
        float px = panel.getPosition().getX();
        float py = panel.getPosition().getY();
        float panelH = panel.getPosition().getHeight();

        float winX = PAD;
        float winY = TAB_BAR_H + TOP_GAP;
        float winW = panel.getPosition().getWidth() - 2f * PAD;
        float winH = panelH - winY - PAD;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Main window backing + frame. Starsector UI / ExampleCustomUIPanel use
        // screen coords with origin at the TOP-LEFT of the panel (y grows down),
        // NOT classic GL bottom-left. getY() is the panel's top edge.
        float winTop = py + winY;
        float winLeft = px + winX;
        GL11.glColor4f(0.035f, 0.045f, 0.065f, 0.92f * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(winLeft,              winTop);
        GL11.glVertex2f(winLeft + winW,       winTop);
        GL11.glVertex2f(winLeft + winW,       winTop + winH);
        GL11.glVertex2f(winLeft,              winTop + winH);
        GL11.glEnd();

        GL11.glColor4f(0.35f, 0.62f, 0.88f, 0.82f * alphaMult);
        GL11.glLineWidth(2.2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(winLeft,              winTop);
        GL11.glVertex2f(winLeft + winW,       winTop);
        GL11.glVertex2f(winLeft + winW,       winTop + winH);
        GL11.glVertex2f(winLeft,              winTop + winH);
        GL11.glEnd();

        // Portrait border — align to the left rail's committed bounds (cached leftY alone can
        // drift from the engine-laid TooltipMaker, which made the frame overlap the bottom bar).
        if (portraitW > 0f && leftRailTip != null && leftRailTip instanceof UIComponentAPI) {
            try {
                PositionAPI rp = ((UIComponentAPI) leftRailTip).getPosition();
                if (rp != null && rp.getWidth() > 2f && rp.getHeight() > 2f) {
                    portraitX = (rp.getX() - px) + (rp.getWidth() - PORTRAIT_W) * 0.5f;
                    portraitY = rp.getY() - py;
                }
            } catch (Throwable ignored) {}
        }

        // Portrait border (vanilla-style cyan accent).
        if (portraitW > 0f) {
            float pTop = py + portraitY;
            GL11.glColor4f(0.50f, 0.78f, 1.00f, 0.85f * alphaMult);
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(px + portraitX - 1f,              pTop - 1f);
            GL11.glVertex2f(px + portraitX + portraitW + 1f,  pTop - 1f);
            GL11.glVertex2f(px + portraitX + portraitW + 1f,  pTop + portraitH + 1f);
            GL11.glVertex2f(px + portraitX - 1f,              pTop + portraitH + 1f);
            GL11.glEnd();
        }

        // --- Viewport clipping -------------------------------------------
        // glScissor() takes FRAMEBUFFER pixel coords (bottom-left origin),
        // but we're working in UI-space coords (top-down, often a different
        // logical size than the framebuffer, e.g. 1920x1080 UI on a
        // 2560x1440 display). To do scissor correctly we have to sample the
        // current GL_VIEWPORT and derive a ui->pixel scale.
        //
        // Empirically this mismatch was what caused the "bottom obscured"
        // bug (and the missing 4th lane): using Display.getHeight() in place
        // of viewport height over-clipped the lower half of content.
        //
        // For the common case where the whole tree fits in viewW (no
        // horizontal scroll), we don't need scissor at all — just render
        // the full content unclipped. Scissor is only applied when the
        // tree is wider than the viewport.
        boolean needsScissor = contentW > viewW + 1f;
        if (needsScissor) {
            try {
                java.nio.IntBuffer vp = org.lwjgl.BufferUtils.createIntBuffer(16);
                GL11.glGetInteger(GL11.GL_VIEWPORT, vp);
                int vpPxX = vp.get(0);
                int vpPxY = vp.get(1);
                int vpPxW = vp.get(2);
                int vpPxH = vp.get(3);
                float uiW = Global.getSettings().getScreenWidth();
                float uiH = Global.getSettings().getScreenHeight();
                if (uiW < 1f) uiW = 1f;
                if (uiH < 1f) uiH = 1f;
                float kx = vpPxW / uiW;
                float ky = vpPxH / uiH;

                float uiLeft   = px + viewX;
                float uiTop    = py + viewY;
                int scLeft   = (int) Math.floor(vpPxX + uiLeft * kx);
                int scBottom = (int) Math.floor(vpPxY + vpPxH - (uiTop + viewH) * ky);
                int scW      = (int) Math.ceil(viewW * kx);
                int scH      = (int) Math.ceil(viewH * ky);
                if (scW < 1) scW = 1;
                if (scH < 1) scH = 1;
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scLeft, scBottom, scW, scH);
            } catch (Throwable t) {
                // Fallback: no scissor rather than risk wrong clipping.
            }
        }

        // Connection lines (edge coords are panel-local top-down, same as nodeCenters).
        if (!edges.isEmpty()) {
            ConnectionLineDrawer.draw(edges, alphaMult, px, py, false);
        }

        // --- Pass 1: icons (textured). Drawn here (renderBelow) so that the
        //     tooltip — which is a child UI element painted AFTER renderBelow
        //     — sits on top of icons and frames as the user expects. ---
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        float nSize = PathTreeBuilder.NODE;
        for (Map.Entry<String, NodeState> entry : nodeStates.entrySet()) {
            String id = entry.getKey();
            NodeState ns = entry.getValue();
            float[] xy = nodeLocal.get(id);
            if (xy == null) continue;
            float cx = px + xy[0] + nSize * 0.5f;
            float cy = py + xy[1] + nSize * 0.5f;
            float iconAlpha = alphaMult * (ns.owned ? 1.0f : ns.unlockable ? 1.0f : ns.pending ? 1.0f : 0.55f);
            drawNodeIcon(ns.node.getIcon(), cx, cy, iconAlpha);
        }

        // --- Pass 2: frames ---
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        for (Map.Entry<String, NodeState> entry : nodeStates.entrySet()) {
            String id = entry.getKey();
            NodeState ns = entry.getValue();
            float[] xy = nodeLocal.get(id);
            if (xy == null) continue;
            float ix = px + xy[0];
            float iy = py + xy[1];
            Color c = nodeBorderColors.get(id);
            if (c == null) c = PathTreeBuilder.COLOR_LOCKED;
            drawNodeFrame(ix, iy, nSize, nSize,
                    c, ns.owned, ns.pending, ns.unlockable, ns.node.isCapstone(), alphaMult);
        }

        if (needsScissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scroll track + thumb just under the viewport.
        drawScrollBar(px, py, scrollXByTree.get(currentTree), alphaMult);

        // XP progress bar inside the bottom bar band.
        drawXpBar(px, py, winX, winW, alphaMult);

        // Tooltip background (drawn last in renderBelow — tooltip text is a
        // child element and therefore paints on top of this bg, and above
        // the scissored icon pass). Prefer the element's committed PositionAPI
        // (screen coords) so the quad matches the engine-placed TooltipMaker.
        if (tooltipElement != null) {
            float tLeft = 0f, tTop = 0f, tW = 0f, tH = 0f;
            boolean have = false;
            if (tooltipElement instanceof UIComponentAPI) {
                try {
                    PositionAPI tip = ((UIComponentAPI) tooltipElement).getPosition();
                    if (tip != null && tip.getWidth() > 0.5f && tip.getHeight() > 0.5f) {
                        tLeft = tip.getX();
                        tTop = tip.getY();
                        tW = tip.getWidth();
                        tH = tip.getHeight();
                        have = true;
                    }
                } catch (Throwable ignored) {}
            }
            if (!have && tooltipLocalX >= 0f) {
                tLeft = px + tooltipLocalX;
                tTop = py + tooltipLocalY;
                tW = tooltipLocalW;
                tH = tooltipLocalH;
                have = true;
            }
            if (have) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(0.02f, 0.05f, 0.09f, 0.95f * alphaMult);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(tLeft,           tTop);
                GL11.glVertex2f(tLeft + tW,      tTop);
                GL11.glVertex2f(tLeft + tW,      tTop + tH);
                GL11.glVertex2f(tLeft,           tTop + tH);
                GL11.glEnd();

                GL11.glColor4f(0.45f, 0.72f, 1.00f, 0.90f * alphaMult);
                GL11.glLineWidth(1.3f);
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex2f(tLeft + 0.5f,           tTop + 0.5f);
                GL11.glVertex2f(tLeft + tW - 0.5f,      tTop + 0.5f);
                GL11.glVertex2f(tLeft + tW - 0.5f,      tTop + tH - 0.5f);
                GL11.glVertex2f(tLeft + 0.5f,           tTop + tH - 0.5f);
                GL11.glEnd();
            }
        }

        GL11.glPopAttrib();
    }

    private void drawXpBar(float px, float py, float winX, float winW, float alphaMult) {
        float barLocalX = winX + PAD + 6f;
        float barW = winW - 2f * PAD - 12f;
        float barH = 10f;
        // Keep the fill under the status line: anchor to the bottom of the bottom bar band.
        float barTop = py + bottomBarTopY + BOTTOM_BAR_H - barH - 8f;
        float barX = px + barLocalX;

        GL11.glColor4f(0.05f, 0.08f, 0.12f, 0.90f * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(barX,        barTop);
        GL11.glVertex2f(barX + barW, barTop);
        GL11.glVertex2f(barX + barW, barTop + barH);
        GL11.glVertex2f(barX,        barTop + barH);
        GL11.glEnd();

        float frac = (bbReq > 0) ? ((float) bbCur / (float) bbReq) : 0f;
        if (frac < 0f) frac = 0f; else if (frac > 1f) frac = 1f;
        float fillW = barW * frac;
        if (fillW > 0.5f) {
            Color tint;
            switch (currentTree) {
                case PILOT:      tint = new Color( 90, 170, 240); break;
                case COMMANDER:  tint = new Color(230, 180,  70); break;
                case ADMIN:      tint = new Color(140, 220, 160); break;
                default:         tint = new Color(180, 180, 180); break;
            }
            GL11.glColor4f(tint.getRed()/255f, tint.getGreen()/255f, tint.getBlue()/255f, 0.75f * alphaMult);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(barX,           barTop + 1f);
            GL11.glVertex2f(barX + fillW,   barTop + 1f);
            GL11.glVertex2f(barX + fillW,   barTop + barH - 1f);
            GL11.glVertex2f(barX,           barTop + barH - 1f);
            GL11.glEnd();
        }

        GL11.glColor4f(0.35f, 0.62f, 0.88f, 0.80f * alphaMult);
        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(barX + 0.5f,          barTop + 0.5f);
        GL11.glVertex2f(barX + barW - 0.5f,   barTop + 0.5f);
        GL11.glVertex2f(barX + barW - 0.5f,   barTop + barH - 0.5f);
        GL11.glVertex2f(barX + 0.5f,          barTop + barH - 0.5f);
        GL11.glEnd();
    }

    private void drawScrollBar(float px, float py, float sx, float alphaMult) {
        float trackTop = py + viewY + viewH + 2f;
        float trackX = px + viewX;
        float trackW = viewW;

        GL11.glColor4f(0.08f, 0.10f, 0.14f, 0.70f * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(trackX,          trackTop);
        GL11.glVertex2f(trackX + trackW, trackTop);
        GL11.glVertex2f(trackX + trackW, trackTop + SCROLL_H);
        GL11.glVertex2f(trackX,          trackTop + SCROLL_H);
        GL11.glEnd();

        if (contentW <= viewW + 0.5f) {
            GL11.glColor4f(0.25f, 0.35f, 0.50f, 0.25f * alphaMult);
            GL11.glLineWidth(1.0f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(trackX + 0.5f,          trackTop + 0.5f);
            GL11.glVertex2f(trackX + trackW - 0.5f, trackTop + 0.5f);
            GL11.glVertex2f(trackX + trackW - 0.5f, trackTop + SCROLL_H - 0.5f);
            GL11.glVertex2f(trackX + 0.5f,          trackTop + SCROLL_H - 0.5f);
            GL11.glEnd();
            return;
        }

        float thumbFrac = Math.max(0.08f, viewW / contentW);
        float thumbW = trackW * thumbFrac;
        float travel = trackW - thumbW;
        float maxScroll = Math.max(1f, contentW - viewW);
        float thumbX = trackX + travel * (sx / maxScroll);

        GL11.glColor4f(0.35f, 0.62f, 0.88f, 0.85f * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(thumbX,          trackTop + 1f);
        GL11.glVertex2f(thumbX + thumbW, trackTop + 1f);
        GL11.glVertex2f(thumbX + thumbW, trackTop + SCROLL_H - 1f);
        GL11.glVertex2f(thumbX,          trackTop + SCROLL_H - 1f);
        GL11.glEnd();

        GL11.glColor4f(0.7f, 0.85f, 1f, 0.80f * alphaMult);
        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(thumbX + 0.5f,          trackTop + 1.5f);
        GL11.glVertex2f(thumbX + thumbW - 0.5f, trackTop + 1.5f);
        GL11.glVertex2f(thumbX + thumbW - 0.5f, trackTop + SCROLL_H - 1.5f);
        GL11.glVertex2f(thumbX + 0.5f,          trackTop + SCROLL_H - 1.5f);
        GL11.glEnd();
    }

    @Override
    public void render(float alphaMult) {
        // Rendering moved to renderBelow so tooltip (a child element drawn
        // after renderBelow) layers on top of icons/frames.
    }

    /**
     * @param cx screen x of cell center
     * @param cy screen y of cell center (y-down, same as ExampleCustomUIPanel)
     */
    private void drawNodeIcon(String path, float cx, float cy, float alphaMult) {
        SpriteAPI sprite = null;
        if (path != null && !path.isEmpty()) {
            try { Global.getSettings().loadTexture(path); } catch (Throwable ignored) {}
            try {
                SpriteAPI s = Global.getSettings().getSprite(path);
                if (s != null && s.getTextureId() != 0) sprite = s;
            } catch (Throwable ignored) {}
        }

        float n = PathTreeBuilder.NODE;
        if (sprite != null) {
            sprite.setSize(n, n);
            sprite.setAlphaMult(alphaMult);
            sprite.setColor(Color.WHITE);
            sprite.renderAtCenter(cx, cy);
            return;
        }

        float x = cx - n * 0.5f;
        float y = cy - n * 0.5f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.14f, 0.18f, 0.24f, 0.85f * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x,     y);
        GL11.glVertex2f(x + n, y);
        GL11.glVertex2f(x + n, y + n);
        GL11.glVertex2f(x,     y + n);
        GL11.glEnd();
        GL11.glColor4f(0.80f, 0.25f, 0.30f, 0.70f * alphaMult);
        GL11.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x + 6f,     y + 6f);
        GL11.glVertex2f(x + n - 6f, y + n - 6f);
        GL11.glVertex2f(x + n - 6f, y + 6f);
        GL11.glVertex2f(x + 6f,     y + n - 6f);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawNodeFrame(float x, float y, float w, float h, Color border,
                               boolean owned, boolean pending, boolean unlockable,
                               boolean capstone, float alphaMult) {
        float r = border.getRed()   / 255f;
        float g = border.getGreen() / 255f;
        float b = border.getBlue()  / 255f;

        float borderAlpha = owned ? 0.95f : pending ? 0.90f : unlockable ? 0.85f : 0.55f;
        float thickness = owned || pending || capstone ? 1.6f : 1.0f;

        GL11.glColor4f(r, g, b, borderAlpha * alphaMult);
        GL11.glLineWidth(thickness);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x + 0.5f,     y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + h - 0.5f);
        GL11.glVertex2f(x + 0.5f,     y + h - 0.5f);
        GL11.glEnd();

        if (owned || pending || unlockable) {
            GL11.glColor4f(r, g, b, 0.30f * alphaMult);
            GL11.glLineWidth(1.0f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(x + 2.5f,     y + 2.5f);
            GL11.glVertex2f(x + w - 2.5f, y + 2.5f);
            GL11.glVertex2f(x + w - 2.5f, y + h - 2.5f);
            GL11.glVertex2f(x + 2.5f,     y + h - 2.5f);
            GL11.glEnd();
        }

        float cl = capstone ? 10f : 6f;
        float cAlpha = owned || pending || unlockable ? 1.0f : 0.6f;
        GL11.glColor4f(r, g, b, cAlpha * alphaMult);
        GL11.glLineWidth(capstone ? 1.8f : 1.2f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x + 0.5f,      y + h - 0.5f); GL11.glVertex2f(x + 0.5f + cl, y + h - 0.5f);
        GL11.glVertex2f(x + 0.5f,      y + h - 0.5f); GL11.glVertex2f(x + 0.5f,      y + h - 0.5f - cl);
        GL11.glVertex2f(x + w - 0.5f,      y + h - 0.5f); GL11.glVertex2f(x + w - 0.5f - cl, y + h - 0.5f);
        GL11.glVertex2f(x + w - 0.5f,      y + h - 0.5f); GL11.glVertex2f(x + w - 0.5f,      y + h - 0.5f - cl);
        GL11.glVertex2f(x + 0.5f,      y + 0.5f); GL11.glVertex2f(x + 0.5f + cl, y + 0.5f);
        GL11.glVertex2f(x + 0.5f,      y + 0.5f); GL11.glVertex2f(x + 0.5f,      y + 0.5f + cl);
        GL11.glVertex2f(x + w - 0.5f,      y + 0.5f); GL11.glVertex2f(x + w - 0.5f - cl, y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f,      y + 0.5f); GL11.glVertex2f(x + w - 0.5f,      y + 0.5f + cl);
        GL11.glEnd();

        if (pending) {
            GL11.glColor4f(r, g, b, 0.90f * alphaMult);
            GL11.glBegin(GL11.GL_QUADS);
            float p = 3f;
            GL11.glVertex2f(x + w - p - 2f, y + h - p - 2f);
            GL11.glVertex2f(x + w - 2f,     y + h - p - 2f);
            GL11.glVertex2f(x + w - 2f,     y + h - 2f);
            GL11.glVertex2f(x + w - p - 2f, y + h - 2f);
            GL11.glEnd();
        }
    }

    // ------------------------------------------------------------ buttons / input

    int sumPendingCost(Set<String> pending) {
        int s = 0;
        TreeDef def = TreeRegistry.get(currentTree);
        for (String id : pending) {
            Node n = def.getNode(id);
            if (n != null) s += n.getCost();
        }
        return s;
    }

    private boolean hasAnyInPreviousTier(int tier, TreeDef def,
                                         Map<String, Object> snap, Set<String> pending) {
        if (tier <= 0) return true;
        int prevTier = tier - 1;
        for (Node n : def.getNodes()) {
            if (n.getTier() != prevTier) continue;
            String k = SkillMemory.purchasedKey(n.getId());
            if (snap.containsKey(k) && Boolean.TRUE.equals(snap.get(k))) return true;
            if (pending.contains(n.getId())) return true;
        }
        return false;
    }

    @Override
    public void buttonPressed(Object id) {
        if (!(id instanceof String)) return;
        String s = (String) id;

        if (s.startsWith("__tab__")) {
            Tree next = Tree.valueOf(s.substring("__tab__".length()));
            switchTo(next);
            return;
        }
        if ("__respec__".equals(s)) {
            // Respec is only actionable if the player has at least one owned
            // node in the current tree.
            if (ownedCountForTree(currentTree) > 0) {
                SkillUISounds.playRespecReset();
                skills_overhaul.level.RespecRunner.run(currentTree);
                rebuild();
            }
            return;
        }
        if ("__confirm__".equals(s)) {
            commitPending();
            rebuild();
            return;
        }
    }

    private void switchTo(Tree next) {
        if (next == currentTree) return;
        pendingByTree.get(currentTree).clear();
        currentTree = next;
        rebuild();
    }

    private boolean inputLogOnce = false;

    /**
     * {@link com.fs.starfarer.api.input.InputEventAPI#getY()} uses bottom-origin screen Y
     * (OpenGL / LWJGL style); {@link PositionAPI} and {@code inTL} use top-down panel space like
     * {@link #nodeLocal}. Without this conversion, vertical hit tests and tooltips track inverted
     * vs. the cursor.
     */
    private float eventToPanelLocalY(com.fs.starfarer.api.input.InputEventAPI e) {
        if (panel == null) return 0f;
        float py = panel.getPosition().getY();
        try {
            float sh = Global.getSettings().getScreenHeight();
            if (sh < 1f) sh = 1f;
            float yFromScreenTop = sh - (float) e.getY();
            return yFromScreenTop - py;
        } catch (Throwable t) {
            return (float) e.getY() - py;
        }
    }

    /** Hit-test in panel-local coords (same space as {@link #nodeLocal}). */
    private String pickNodeAtPanelLocal(float lx, float ly) {
        float n = PathTreeBuilder.NODE;
        for (Map.Entry<String, float[]> e : nodeLocal.entrySet()) {
            float[] xy = e.getValue();
            if (xy == null) continue;
            if (lx >= xy[0] && lx <= xy[0] + n && ly >= xy[1] && ly <= xy[1] + n) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Authoritative hit-test in engine/UI space (same logic vanilla buttons use). */
    private String pickNodeAtEvent(com.fs.starfarer.api.input.InputEventAPI e) {
        for (Map.Entry<String, PositionAPI> entry : nodePositions.entrySet()) {
            PositionAPI p = entry.getValue();
            if (p != null && p.containsEvent(e)) return entry.getKey();
        }
        return null;
    }

    private static boolean isLeftMouseDown(com.fs.starfarer.api.input.InputEventAPI e) {
        try {
            if (e.isLMBDownEvent()) return true;
            if (e.isMouseDownEvent() && e.getEventValue() == 0) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public void processInput(java.util.List<com.fs.starfarer.api.input.InputEventAPI> events) {
        if (events == null || panel == null) return;

        if (!inputLogOnce) {
            inputLogOnce = true;
            Global.getLogger(CharacterTabPanelPlugin.class).info(
                    "[SkillsOverhaul/UI] processInput is live, nodePositions=" + nodePositions.size()
                    + " events/frame=" + events.size());
        }

        float pxx = panel.getPosition().getX();
        float pyy = panel.getPosition().getY();

        // Track hover + last cursor (tooltip Y follows mouse while over a node).
        String hoverNow = null;
        boolean sawMouse = false;
        for (com.fs.starfarer.api.input.InputEventAPI e : events) {
            if (!e.isMouseMoveEvent() && !e.isMouseEvent()) continue;
            sawMouse = true;
            float lx = e.getX() - pxx;
            float ly = eventToPanelLocalY(e);
            lastPanelMouseX = lx;
            lastPanelMouseY = ly;
            // Use engine-space hit rect first; panel-local math is fallback only.
            String h = pickNodeAtEvent(e);
            if (h == null) h = pickNodeAtPanelLocal(lx, ly);
            hoverNow = h;
        }
        if (!sawMouse) {
            hoverNow = hoveredNodeId;
        }
        if ((hoverNow == null && hoveredNodeId != null)
                || (hoverNow != null && !hoverNow.equals(hoveredNodeId))) {
            hoveredNodeId = hoverNow;
            updateTooltip();
        } else if (sawMouse && hoveredNodeId != null && tooltipElement != null) {
            repositionTooltip();
        }

        for (com.fs.starfarer.api.input.InputEventAPI e : events) {
            if (e.isConsumed()) continue;

            // Tab hotkeys 1..N — switch between trees.
            if (e.isKeyDownEvent()) {
                int key = e.getEventValue();
                Tree[] trees = Tree.values();
                for (int i = 0; i < trees.length; i++) {
                    boolean match = (key == (org.lwjgl.input.Keyboard.KEY_1 + i))
                                 || (key == (org.lwjgl.input.Keyboard.KEY_NUMPAD1 + i));
                    if (match) {
                        switchTo(trees[i]);
                        e.consume();
                        return;
                    }
                }
                if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                    boolean anyPending = false;
                    for (Set<String> pending : pendingByTree.values())
                        if (!pending.isEmpty()) { anyPending = true; break; }
                    if (anyPending) {
                        for (Set<String> pending : pendingByTree.values()) pending.clear();
                        e.consume();
                        rebuild();
                        return;
                    }
                }
            }

            if (isScrollEvent(e)) {
                float px = panel.getPosition().getX();
                float py = panel.getPosition().getY();
                float mx = e.getX() - px;
                float my = eventToPanelLocalY(e);
                boolean overView = mx >= viewX && mx <= viewX + viewW
                                && my >= viewY - SCROLL_H - 4f && my <= viewY + viewH;
                if (overView) {
                    float delta = scrollDelta(e);
                    if (delta != 0f) {
                        float cur = scrollXByTree.get(currentTree);
                        float maxScroll = Math.max(0f, contentW - viewW);
                        cur = Math.max(0f, Math.min(maxScroll, cur + delta));
                        scrollXByTree.put(currentTree, cur);
                        e.consume();
                        rebuild();
                        return;
                    }
                }
            }

            if (isLeftMouseDown(e)) {
                float lx = e.getX() - pxx;
                float ly = eventToPanelLocalY(e);
                String clickedId = pickNodeAtEvent(e);
                if (clickedId == null) clickedId = pickNodeAtPanelLocal(lx, ly);
                if (clickedId != null) {
                    Global.getLogger(CharacterTabPanelPlugin.class).info(
                            "[SkillsOverhaul/UI] click " + clickedId + " @ (" + e.getX() + "," + e.getY() + ")");
                    e.consume();
                    handleNodeClick(clickedId);
                }
            }
        }
    }

    private boolean isScrollEvent(com.fs.starfarer.api.input.InputEventAPI e) {
        try {
            if (e.isMouseScrollEvent()) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private float scrollDelta(com.fs.starfarer.api.input.InputEventAPI e) {
        try {
            int v = e.getEventValue();
            return -(v / 120f) * 80f;
        } catch (Throwable t) {
            return 0f;
        }
    }

    private void handleNodeClick(String nodeId) {
        Set<String> pending = pendingByTree.get(currentTree);
        if (pending.contains(nodeId)) {
            removePendingWithDependents(nodeId, pending);
            rebuild();
            return;
        }

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        String purK = SkillMemory.purchasedKey(nodeId);
        if (mem.contains(purK) && mem.getBoolean(purK)) return;

        TreeDef def = TreeRegistry.get(currentTree);
        Node clicked = def.getNode(nodeId);
        if (clicked == null) return;

        Map<String, Object> snap = new HashMap<String, Object>();
        for (Node n : def.getNodes()) {
            String k = SkillMemory.purchasedKey(n.getId());
            if (mem.contains(k) && mem.getBoolean(k)) snap.put(k, Boolean.TRUE);
        }
        String pk = SkillMemory.pointsKey(currentTree);
        if (mem.contains(pk)) snap.put(pk, mem.getFloat(pk));

        if (!hasAnyInPreviousTier(clicked.getTier(), def, snap, pending)) return;
        if (!skills_overhaul.tree.PrereqValidator.canPurchaseWithPending(def, snap, pending, nodeId)) return;

        int pts = SkillMemory.getPoints(snap, currentTree);
        if (sumPendingCost(pending) + clicked.getCost() > pts) {
            Global.getLogger(CharacterTabPanelPlugin.class).warn(
                    "[SkillsOverhaul/UI] over budget: can't pend " + nodeId);
            return;
        }
        pending.add(nodeId);
        SkillUISounds.playTierSkillSound(currentTree, clicked.getTier(), 0.55f);
        rebuild();
    }

    private void commitPending() {
        Set<String> pending = pendingByTree.get(currentTree);
        if (pending.isEmpty()) return;
        TreeDef def = TreeRegistry.get(currentTree);
        List<String> order;
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
                        "[SkillsOverhaul/UI] purchase failed for " + nodeId);
                break;
            }
        }
        pending.clear();
    }

    private void removePendingWithDependents(String removed, Set<String> pending) {
        TreeDef def = TreeRegistry.get(currentTree);
        Set<String> toRemove = new HashSet<String>();
        toRemove.add(removed);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String id : new ArrayList<String>(pending)) {
                if (toRemove.contains(id)) continue;
                Node n = def.getNode(id);
                if (n == null) continue;
                for (String pre : n.getPrereqs()) {
                    if (toRemove.contains(pre)) {
                        toRemove.add(id);
                        changed = true;
                        break;
                    }
                }
            }
        }
        pending.removeAll(toRemove);
    }
}
