package skills_overhaul.ui.view;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import skills_overhaul.basics.ActiveLaneService;
import skills_overhaul.effect.NodeEffectApplier;
import skills_overhaul.level.GameSpend;
import skills_overhaul.level.PurchasePlan;
import skills_overhaul.level.RespecRunner;
import skills_overhaul.level.XPCurve;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeRegistry;
import skills_overhaul.ui.ConnectionLineDrawer;
import skills_overhaul.ui.PanelCoords;
import skills_overhaul.ui.SkillUISounds;
import skills_overhaul.ui.layout.TreeLayout;
import skills_overhaul.ui.model.NodeStatus;
import skills_overhaul.ui.model.SkillTreeModel;

import java.awt.Color;
import java.util.*;

/**
 * Skills panel view: header (tab strip, left rail, bottom bar) plus a zoomable, pannable
 * tree viewport, all driven by {@link SkillTreeModel} (state), {@link TreeLayout}
 * (geometry and view transform) and {@link GameSpend#commit} (purchases).
 *
 * <p>Child components are created in the constructor and on a tab switch only. Every other
 * change (hover, click, zoom, pan, commit, respec) refreshes cached state, label text and
 * anchor positions; {@code render}, {@code renderBelow} and {@code advance} never add or
 * remove components, load textures or rebuild edges.
 *
 * <p>Host-agnostic: the view creates its own panel of the given size; a host mounts
 * {@link #getPanel()} and calls {@link #discard()} when it removes it.
 *
 * <p>Coordinates: panel-local space is bottom-up (see {@link PanelCoords}). The tree
 * viewport's bottom-left corner sits at panel-local {@code (vpX, vpY)}; TreeLayout's screen
 * space is viewport-local, so panel-local = {@code (vpX, vpY) + contentToScreen(content)}.
 */
public final class SkillTreeView extends BaseCustomUIPanelPlugin {

    private static final float TAB_BAR_H    = 24f;
    private static final float TAB_W        = 110f;
    private static final float TAB_GAP      = 4f;
    private static final float LEFT_RAIL_W  = 240f;
    private static final float BOTTOM_BAR_H = 44f;
    private static final float PAD          = 14f;
    private static final float TOP_GAP      = 4f;
    private static final float PORTRAIT_W   = 112f;

    /** Cursor travel (px) above which a left press becomes a pan drag instead of a click. */
    private static final float DRAG_THRESHOLD = 6f;
    private static final double WHEEL_ZOOM_STEP = 1.1;
    private static final float FLASH_SECONDS = 0.4f;
    private static final Color FLASH_RED = new Color(235, 60, 60);
    private static final Color ACTIVE_RING = new Color(255, 209, 64);

    private static final String BTN_TAB_PREFIX = "__tab__";
    private static final String BTN_RESPEC = "__respec__";
    private static final String BTN_CONFIRM = "__confirm__";

    private final CustomPanelAPI panel;
    private final float panelW, panelH;

    // --- Fixed panel geometry (panel-local; y values are inTL top pads unless noted) ---
    private final float winX, winY, winW, winH;
    private final float leftX, leftY, leftW, leftH;
    private final float bottomBarTopY;
    /** Tree viewport: panel-local bottom-left (bottom-up) and size. */
    private final float vpX, vpY, vpW, vpH;

    // --- Per-tab state (rebuilt by buildTab) ----------------------------------------
    private Tree currentTab = Tree.PILOT;
    private List<TreeDef> defs = Collections.emptyList();
    private TreeLayout layout;
    private SkillTreeModel model;
    private final Map<String, Node> nodesById = new LinkedHashMap<String, Node>();
    private final List<UIComponentAPI> children = new ArrayList<UIComponentAPI>();
    private final Map<String, CustomPanelAPI> anchors = new LinkedHashMap<String, CustomPanelAPI>();
    private final List<TooltipMakerAPI> columnHeaders = new ArrayList<TooltipMakerAPI>();
    private TooltipMakerAPI rail;
    private LabelAPI storyPointsLabel, freeUniLabel, migratedLabel, bottomLabel;
    private ButtonAPI respecButton, confirmButton;

    // --- Derived state (refreshed on events, read every frame) -------------------------
    private Map<String, NodeStatus> statuses = Collections.emptyMap();
    private final Set<String> ownedOrPending = new HashSet<String>();
    private final List<ConnectionLineDrawer.Edge> edges = new ArrayList<ConnectionLineDrawer.Edge>();
    private List<String> activeIds = Collections.emptyList();
    private long xpCur, xpReq = 1;

    /** Sprite per icon path; a null value records a path whose texture could not load. */
    private final Map<String, SpriteAPI> sprites = new HashMap<String, SpriteAPI>();
    /** Remaining red-flash seconds per node id. */
    private final Map<String, Float> flashes = new HashMap<String, Float>();

    // --- Input state ---------------------------------------------------------------------
    private boolean pressing, dragging;
    private float pressX, pressY, lastX, lastY;

    public SkillTreeView(float width, float height) {
        panelW = Math.max(0f, width);
        panelH = Math.max(0f, height);
        panel = Global.getSettings().createCustom(panelW, panelH, this);

        winX = PAD;
        winY = TAB_BAR_H + TOP_GAP;
        winW = panelW - 2f * PAD;
        winH = panelH - winY - PAD;
        leftX = winX + PAD;
        leftY = winY + PAD;
        leftW = Math.min(LEFT_RAIL_W, Math.max(200f, winW * 0.22f));
        leftH = Math.max(0f, winH - 2f * PAD - BOTTOM_BAR_H);
        bottomBarTopY = leftY + leftH + PAD;

        float viewLeft = leftX + leftW + PAD;
        vpX = viewLeft;
        vpW = Math.max(0f, (winX + winW) - viewLeft - PAD);
        vpH = leftH;
        vpY = PanelCoords.localYFromTop(panelH, leftY, vpH);

        buildTab();
    }

    /** The panel to mount. */
    public CustomPanelAPI getPanel() { return panel; }

    public Tree getCurrentTab() { return currentTab; }

    /** Clears pending purchases; hosts call this when they remove the panel. */
    public void discard() {
        if (model != null && !model.getPending().isEmpty()) {
            model = new SkillTreeModel(currentTab, defs, GameSpend.snapshot(currentTab));
        }
        flashes.clear();
        pressing = false;
        dragging = false;
    }

    // ================================================================== build (tab switch)

    /** Creates every child component for {@link #currentTab}. Construction and tab switch only. */
    private void buildTab() {
        for (UIComponentAPI c : children) {
            try { panel.removeComponent(c); } catch (Throwable ignored) {}
        }
        children.clear();
        anchors.clear();
        columnHeaders.clear();
        nodesById.clear();
        flashes.clear();
        pressing = false;
        dragging = false;

        List<TreeDef> d = null;
        try { d = TreeRegistry.getTrees(currentTab); } catch (Throwable ignored) {}
        defs = d == null ? Collections.<TreeDef>emptyList() : d;
        for (TreeDef def : defs) {
            if (def == null) continue;
            for (Node n : def.getNodes()) {
                if (!nodesById.containsKey(n.getId())) nodesById.put(n.getId(), n);
            }
        }
        layout = new TreeLayout(defs, vpW, vpH);
        model = new SkillTreeModel(currentTab, defs, GameSpend.snapshot(currentTab));
        preloadSprites();

        buildTabStrip();
        buildRail();
        buildBottomBar();
        buildColumnHeaders();
        buildAnchors();

        refreshState();
        positionViewChildren();
    }

    private void buildTabStrip() {
        float tabBarW = Tree.values().length * (TAB_W + TAB_GAP);
        float tabX = (panelW - tabBarW) / 2f;
        Color baseC = safeColor(new Color(140, 200, 240), true);
        Color bgC = safeColor(new Color(18, 35, 60), false);
        Color hotkeyC = new Color(255, 220, 70);
        int idx = 1;
        for (Tree t : Tree.values()) {
            TooltipMakerAPI tabBtn = panel.createUIElement(TAB_W, TAB_BAR_H, false);
            ButtonAPI btn = tabBtn.addButton(t.key.toUpperCase(), BTN_TAB_PREFIX + t.name(),
                    baseC, bgC, Alignment.MID, CutStyle.C2_MENU, TAB_W, TAB_BAR_H, 0f);
            if (btn != null && t == currentTab) btn.setChecked(true);
            addElement(tabBtn, tabX, 0f);

            TooltipMakerAPI hot = panel.createUIElement(28f, 16f, false);
            hot.addPara("[%s]", 0f, hotkeyC, "" + idx);
            addElement(hot, tabX + TAB_W - 26f, 4f);

            tabX += TAB_W + TAB_GAP;
            idx++;
        }
    }

    private void buildRail() {
        TooltipMakerAPI tip = panel.createUIElement(leftW, leftH, false);
        PersonAPI player = Global.getSector().getPlayerPerson();
        if (player != null) {
            String portrait = player.getPortraitSprite();
            if (portrait != null && !portrait.isEmpty()) {
                try { tip.addImage(portrait, PORTRAIT_W, 0f); } catch (Throwable ignored) {}
            }
            tip.addPara(player.getNameString(), 6f);
        }
        tip.addPara("", 2f);
        storyPointsLabel = tip.addPara("Story points: 0", 4f);
        freeUniLabel = tip.addPara("Free universal points: 0", 6f);
        migratedLabel = tip.addPara("Migrated points: 0", 2f);
        tip.addPara("", 6f);
        tip.addPara("Keys 1-3 switch tabs", new Color(150, 160, 180), 2f);

        // Action buttons last so later widgets never stack hit targets on top of them.
        tip.addPara("", 8f);
        float btnW = Math.max(120f, leftW - 10f);
        float btnH = 28f;
        confirmButton = tip.addButton("Confirm (0 pts)", BTN_CONFIRM,
                new Color(60, 160, 90), new Color(30, 90, 50),
                Alignment.MID, CutStyle.C2_MENU, btnW, btnH, 4f);
        respecButton = tip.addButton("Respec (1 SP)", BTN_RESPEC,
                new Color(180, 60, 60), new Color(100, 30, 30),
                Alignment.MID, CutStyle.C2_MENU, btnW, btnH, 4f);
        addElement(tip, leftX, leftY);
        rail = tip;
    }

    private void buildBottomBar() {
        TooltipMakerAPI tip = panel.createUIElement(winW - 2f * PAD, BOTTOM_BAR_H, false);
        bottomLabel = tip.addPara("", 2f);
        addElement(tip, winX + PAD, bottomBarTopY);
    }

    private void buildColumnHeaders() {
        double colW = layout.getColumns().isEmpty() ? 0 : layout.getColumns().get(0).w;
        float hh = (float) layout.getHeaderHeight();
        Color accent = new Color(150, 200, 240);
        for (TreeDef def : layout.getTreeDefs()) {
            TooltipMakerAPI header = panel.createUIElement((float) colW, hh, false);
            try { header.setParaFontVictor14(); } catch (Throwable ignored) {}
            header.addPara(prettyTreeName(def.getTree()), accent, 0f);
            try { header.setParaFontDefault(); } catch (Throwable ignored) {}
            addElement(header, 0f, 0f);
            columnHeaders.add(header);
        }
    }

    /** One invisible anchor panel per node carrying its native tooltip. */
    private void buildAnchors() {
        for (TreeLayout.NodeRect nr : layout.getNodes()) {
            if (anchors.containsKey(nr.id)) continue;
            final String id = nr.id;
            final Node node = nodesById.get(id);
            if (node == null) continue;
            CustomPanelAPI anchor = panel.createCustomPanel(1f, 1f, new BaseCustomUIPanelPlugin());
            panel.addComponent(anchor);
            children.add(anchor);
            anchors.put(id, anchor);
            NodeTooltip tc = new NodeTooltip(new NodeTooltip.Source() {
                public NodeTooltipContent get() {
                    return NodeTooltipContent.build(node, statuses.get(id), ownedOrPending);
                }
            });
            // Content reads the cached statuses, so recreating it each frame stays cheap and
            // the tooltip follows a click on the hovered node.
            rail.addTooltipTo(tc, anchor, TooltipMakerAPI.TooltipLocation.RIGHT, true);
        }
    }

    private void addElement(TooltipMakerAPI el, float x, float yTop) {
        PositionAPI pos = panel.addUIElement(el);
        if (pos != null) pos.inTL(x, yTop);
        children.add(el);
    }

    private void preloadSprites() {
        for (Node n : nodesById.values()) {
            String path = n.getIcon();
            if (path == null || path.isEmpty() || sprites.containsKey(path)) continue;
            SpriteAPI s = null;
            try {
                s = Global.getSettings().getSprite(path);
                if (s == null || s.getTextureId() <= 0) {
                    Global.getSettings().loadTexture(path);
                    s = Global.getSettings().getSprite(path);
                }
                if (s != null && s.getTextureId() <= 0) s = null;
            } catch (Throwable t) {
                s = null;
            }
            sprites.put(path, s);
        }
    }

    // ================================================================== refresh (events)

    /**
     * Recomputes statuses, edge colours, active slots, labels and buttons from the model and
     * memory. Runs on construction, tab switch, click, commit, respec and Esc; never per frame.
     */
    private void refreshState() {
        statuses = model.statuses();
        ownedOrPending.clear();
        for (Map.Entry<String, NodeStatus> e : statuses.entrySet()) {
            NodeStatus s = e.getValue();
            if (s != null && (s.state == NodeStatus.State.OWNED || s.state == NodeStatus.State.PENDING)) {
                ownedOrPending.add(e.getKey());
            }
        }

        edges.clear();
        for (TreeLayout.Edge e : layout.getEdges()) {
            NodeStatus parent = statuses.get(e.fromId);
            NodeStatus child = statuses.get(e.toId);
            boolean parentMet = isState(parent, NodeStatus.State.OWNED) || isState(parent, NodeStatus.State.PENDING);
            Color c;
            if (isState(parent, NodeStatus.State.OWNED) && isState(child, NodeStatus.State.OWNED)) {
                c = UiColors.OWNED;
            } else if (isState(child, NodeStatus.State.PENDING) && parentMet) {
                c = UiColors.PENDING;
            } else if (isState(child, NodeStatus.State.UNLOCKABLE) && parentMet) {
                c = UiColors.UNLOCKABLE;
            } else {
                c = UiColors.LOCKED;
            }
            edges.add(new ConnectionLineDrawer.Edge((float) e.x1, (float) e.y1, (float) e.x2, (float) e.y2,
                    c, false, parentMet));
        }

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        try {
            activeIds = new ArrayList<String>(ActiveLaneService.getActive(mem));
        } catch (Throwable t) {
            activeIds = Collections.emptyList();
        }
        refreshLabels(mem);
    }

    private void refreshLabels(MemoryAPI mem) {
        int sp = 0;
        try { sp = Global.getSector().getPlayerStats().getStoryPoints(); } catch (Throwable ignored) {}
        float freeUni = mem.contains(SkillMemory.freeUniKey()) ? mem.getFloat(SkillMemory.freeUniKey()) : 0f;
        String migK = "$skills.points.universal_migrated_unallocated";
        float migPool = mem.contains(migK) ? mem.getFloat(migK) : 0f;
        if (storyPointsLabel != null) storyPointsLabel.setText("Story points: " + sp);
        if (freeUniLabel != null) freeUniLabel.setText("Free universal points: " + (int) freeUni);
        if (migratedLabel != null) migratedLabel.setText("Migrated points: " + (int) migPool);

        Set<String> pending = model.getPending();
        int cost = 0;
        for (String id : pending) {
            Node n = nodesById.get(id);
            if (n != null) cost += n.getCost();
        }
        if (confirmButton != null) {
            confirmButton.setText("Confirm (" + cost + " pts)");
            confirmButton.setEnabled(!pending.isEmpty());
        }
        if (respecButton != null) {
            boolean canRespec = pending.isEmpty() && ownedCount() > 0;
            respecButton.setText(canRespec ? "Respec (1 SP)" : "Respec (n/a)");
            respecButton.setEnabled(canRespec);
        }

        Map<String, Object> m = new HashMap<String, Object>();
        String lk = SkillMemory.levelKey(currentTab);
        String pk = SkillMemory.pointsKey(currentTab);
        if (mem.contains(lk)) m.put(lk, mem.getFloat(lk));
        if (mem.contains(pk)) m.put(pk, mem.getFloat(pk));
        int level = SkillMemory.getLevel(m, currentTab);
        int pts = SkillMemory.getPoints(m, currentTab);
        long xp = (long) SkillMemory.getXP(mem, currentTab);
        long req = XPCurve.xpForLevel(level + 1);
        long cur = xp - XPCurve.cumulativeXP(level);
        xpCur = Math.max(0, cur);
        xpReq = req <= 0 ? 1 : req;
        if (bottomLabel != null) {
            bottomLabel.setText(currentTab.key.toUpperCase()
                    + "   Level " + level
                    + "   " + xpCur + " / " + xpReq + " XP"
                    + "   (" + pts + " points)");
        }
    }

    private int ownedCount() {
        int n = 0;
        for (NodeStatus s : statuses.values()) if (isState(s, NodeStatus.State.OWNED)) n++;
        return n;
    }

    /** Builds a fresh model (new snapshot, empty pending) and refreshes. */
    private void reloadModel() {
        model = new SkillTreeModel(currentTab, defs, GameSpend.snapshot(currentTab));
        refreshState();
    }

    /**
     * Moves anchors and column headers to the current zoom and pan. UI children do not follow
     * the GL transform, so this runs after every zoom or pan change; it never rebuilds them.
     * An anchor covers only the visible part of its node; a node outside the viewport parks
     * its anchor off the panel so its tooltip cannot open over the rail.
     */
    private void positionViewChildren() {
        float z = (float) layout.getZoom();
        for (TreeLayout.NodeRect nr : layout.getNodes()) {
            CustomPanelAPI a = anchors.get(nr.id);
            if (a == null) continue;
            double[] s = layout.contentToScreen(nr.rect.x, nr.rect.y);
            float x0 = Math.max(0f, (float) s[0]);
            float y0 = Math.max(0f, (float) s[1]);
            float x1 = Math.min(vpW, (float) s[0] + (float) nr.rect.w * z);
            float y1 = Math.min(vpH, (float) s[1] + (float) nr.rect.h * z);
            PositionAPI p = a.getPosition();
            if (x1 - x0 < 1f || y1 - y0 < 1f) {
                p.setSize(1f, 1f);
                p.inBL(-10000f, -10000f);
            } else {
                p.setSize(x1 - x0, y1 - y0);
                p.inBL(vpX + x0, vpY + y0);
            }
        }

        List<TreeLayout.Rect> cols = layout.getColumns();
        double hh = layout.getHeaderHeight();
        for (int i = 0; i < columnHeaders.size() && i < cols.size(); i++) {
            TreeLayout.Rect col = cols.get(i);
            double[] tl = layout.contentToScreen(col.x, col.y + col.h);
            float hx = vpX + (float) tl[0];
            float hTopLocal = vpY + (float) tl[1];
            TooltipMakerAPI h = columnHeaders.get(i);
            PositionAPI p = h.getPosition();
            boolean visible = tl[0] >= -1 && tl[0] + col.w * z <= vpW + 1
                    && tl[1] <= vpH + 1 && tl[1] - hh >= -1;
            p.inBL(hx + 4f, hTopLocal - (float) hh);
            h.setOpacity(visible ? 1f : 0f);
        }
    }

    // ================================================================== rendering

    @Override
    public void advance(float amount) {
        if (flashes.isEmpty()) return;
        Iterator<Map.Entry<String, Float>> it = flashes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> e = it.next();
            float left = e.getValue() - amount;
            if (left <= 0f) it.remove();
            else e.setValue(left);
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        float px = panel.getPosition().getX();
        float py = panel.getPosition().getY();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_SCISSOR_BIT
                | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Window backing and frame.
        float winLeft = PanelCoords.screenX(px, winX);
        float winBottom = PanelCoords.screenY(py, PanelCoords.localYFromTop(panelH, winY, winH));
        GL11.glColor4f(0.035f, 0.045f, 0.065f, 0.92f * alphaMult);
        quad(winLeft, winBottom, winW, winH);
        GL11.glColor4f(0.35f, 0.62f, 0.88f, 0.82f * alphaMult);
        GL11.glLineWidth(2.2f);
        loop(winLeft, winBottom, winW, winH, 0f);

        drawPortraitBorder(px, py, alphaMult);
        drawXpBar(px, py, alphaMult);

        boolean scissor = enableViewportScissor(px, py);

        GL11.glPushMatrix();
        GL11.glTranslatef(px + vpX + (float) layout.getOffsetX(), py + vpY + (float) layout.getOffsetY(), 0f);
        float z = (float) layout.getZoom();
        GL11.glScalef(z, z, 1f);
        drawTree(alphaMult);
        GL11.glPopMatrix();

        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopAttrib();
    }

    /** Draws edges, icons, frames, active rings and flashes in content space (GL transform applied). */
    private void drawTree(float alphaMult) {
        if (!edges.isEmpty()) ConnectionLineDrawer.draw(edges, alphaMult, 0f, 0f);

        float n = (float) layout.getNodeSize();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        for (TreeLayout.NodeRect nr : layout.getNodes()) {
            Node node = nodesById.get(nr.id);
            if (node == null) continue;
            NodeStatus s = statuses.get(nr.id);
            float a = isState(s, NodeStatus.State.BLOCKED) || s == null ? 0.55f : 1.0f;
            drawNodeIcon(node.getIcon(), (float) nr.rect.centerX(), (float) nr.rect.centerY(), n, alphaMult * a);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        for (TreeLayout.NodeRect nr : layout.getNodes()) {
            Node node = nodesById.get(nr.id);
            if (node == null) continue;
            drawNodeFrame((float) nr.rect.x, (float) nr.rect.y, n, n, statuses.get(nr.id),
                    node.isCapstone(), alphaMult);
        }

        if (!activeIds.isEmpty()) {
            GL11.glLineWidth(2.0f);
            color(ACTIVE_RING, 0.90f * alphaMult);
            for (String id : activeIds) {
                TreeLayout.Rect r = layout.getNodeRect(id);
                if (r == null) continue;
                loop((float) r.x - 3f, (float) r.y - 3f, (float) r.w + 6f, (float) r.h + 6f, 0f);
            }
        }

        for (Map.Entry<String, Float> e : flashes.entrySet()) {
            TreeLayout.Rect r = layout.getNodeRect(e.getKey());
            if (r == null) continue;
            float k = Math.min(1f, e.getValue() / FLASH_SECONDS);
            color(FLASH_RED, 0.55f * k * alphaMult);
            quad((float) r.x, (float) r.y, (float) r.w, (float) r.h);
            color(FLASH_RED, k * alphaMult);
            GL11.glLineWidth(2.0f);
            loop((float) r.x, (float) r.y, (float) r.w, (float) r.h, 0.5f);
        }
    }

    private void drawNodeIcon(String path, float cx, float cy, float size, float alpha) {
        SpriteAPI sprite = path == null ? null : sprites.get(path);
        if (sprite != null) {
            sprite.setSize(size, size);
            // Sprites are shared instances: set alpha every frame.
            sprite.setAlphaMult(alpha);
            sprite.setColor(Color.WHITE);
            sprite.renderAtCenter(cx, cy);
            return;
        }
        float x = cx - size * 0.5f;
        float y = cy - size * 0.5f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.14f, 0.18f, 0.24f, 0.85f * alpha);
        quad(x, y, size, size);
        GL11.glColor4f(0.80f, 0.25f, 0.30f, 0.70f * alpha);
        GL11.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x + 6f, y + 6f);
        GL11.glVertex2f(x + size - 6f, y + size - 6f);
        GL11.glVertex2f(x + size - 6f, y + 6f);
        GL11.glVertex2f(x + 6f, y + size - 6f);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawNodeFrame(float x, float y, float w, float h, NodeStatus s,
                               boolean capstone, float alphaMult) {
        boolean owned = isState(s, NodeStatus.State.OWNED);
        boolean pending = isState(s, NodeStatus.State.PENDING);
        boolean unlockable = isState(s, NodeStatus.State.UNLOCKABLE);
        Color border = owned ? (capstone ? UiColors.CAPSTONE : UiColors.OWNED)
                : pending ? UiColors.PENDING
                : unlockable ? UiColors.UNLOCKABLE
                : UiColors.LOCKED;

        float borderAlpha = owned ? 0.95f : pending ? 0.90f : unlockable ? 0.85f : 0.55f;
        GL11.glLineWidth(owned || pending || capstone ? 1.6f : 1.0f);
        color(border, borderAlpha * alphaMult);
        loop(x, y, w, h, 0.5f);

        if (owned || pending || unlockable) {
            color(border, 0.30f * alphaMult);
            GL11.glLineWidth(1.0f);
            loop(x, y, w, h, 2.5f);
        }

        float cl = capstone ? 10f : 6f;
        color(border, (owned || pending || unlockable ? 1.0f : 0.6f) * alphaMult);
        GL11.glLineWidth(capstone ? 1.8f : 1.2f);
        float l = x + 0.5f, r = x + w - 0.5f, b = y + 0.5f, t = y + h - 0.5f;
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(l, t); GL11.glVertex2f(l + cl, t);
        GL11.glVertex2f(l, t); GL11.glVertex2f(l, t - cl);
        GL11.glVertex2f(r, t); GL11.glVertex2f(r - cl, t);
        GL11.glVertex2f(r, t); GL11.glVertex2f(r, t - cl);
        GL11.glVertex2f(l, b); GL11.glVertex2f(l + cl, b);
        GL11.glVertex2f(l, b); GL11.glVertex2f(l, b + cl);
        GL11.glVertex2f(r, b); GL11.glVertex2f(r - cl, b);
        GL11.glVertex2f(r, b); GL11.glVertex2f(r, b + cl);
        GL11.glEnd();

        if (pending) {
            color(border, 0.90f * alphaMult);
            quad(x + w - 5f, y + h - 5f, 3f, 3f);
        }
    }

    private void drawPortraitBorder(float px, float py, float alphaMult) {
        if (rail == null) return;
        PositionAPI rp = rail.getPosition();
        if (rp == null || rp.getWidth() <= 2f || rp.getHeight() <= 2f) return;
        if (Global.getSector().getPlayerPerson() == null) return;
        // The portrait is the rail's first item, so it sits at the rail's top edge.
        float x = rp.getX() + (rp.getWidth() - PORTRAIT_W) * 0.5f;
        float y = rp.getY() + rp.getHeight() - PORTRAIT_W;
        GL11.glColor4f(0.50f, 0.78f, 1.00f, 0.85f * alphaMult);
        GL11.glLineWidth(1.5f);
        loop(x - 1f, y - 1f, PORTRAIT_W + 2f, PORTRAIT_W + 2f, 0f);
    }

    private void drawXpBar(float px, float py, float alphaMult) {
        float barW = winW - 2f * PAD - 12f;
        float barH = 10f;
        float barX = PanelCoords.screenX(px, winX + PAD + 6f);
        float barBottom = PanelCoords.screenY(py,
                PanelCoords.localYFromTop(panelH, bottomBarTopY + BOTTOM_BAR_H - barH - 8f, barH));

        GL11.glColor4f(0.05f, 0.08f, 0.12f, 0.90f * alphaMult);
        quad(barX, barBottom, barW, barH);

        float frac = xpReq > 0 ? (float) xpCur / (float) xpReq : 0f;
        frac = Math.max(0f, Math.min(1f, frac));
        float fillW = barW * frac;
        if (fillW > 0.5f) {
            Color tint;
            switch (currentTab) {
                case PILOT:     tint = new Color(90, 170, 240); break;
                case COMMANDER: tint = new Color(230, 180, 70); break;
                case ADMIN:     tint = new Color(140, 220, 160); break;
                default:        tint = new Color(180, 180, 180); break;
            }
            color(tint, 0.75f * alphaMult);
            quad(barX, barBottom + 1f, fillW, barH - 2f);
        }

        GL11.glColor4f(0.35f, 0.62f, 0.88f, 0.80f * alphaMult);
        GL11.glLineWidth(1.0f);
        loop(barX, barBottom, barW, barH, 0.5f);
    }

    /**
     * Clips to the tree viewport. glScissor takes framebuffer pixels while UI space can be a
     * different logical size, so scale by GL_VIEWPORT over the UI screen size.
     */
    private boolean enableViewportScissor(float px, float py) {
        try {
            java.nio.IntBuffer vp = org.lwjgl.BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, vp);
            float uiW = Math.max(1f, Global.getSettings().getScreenWidth());
            float uiH = Math.max(1f, Global.getSettings().getScreenHeight());
            float kx = vp.get(2) / uiW;
            float ky = vp.get(3) / uiH;
            int scLeft = (int) Math.floor(vp.get(0) + PanelCoords.screenX(px, vpX) * kx);
            int scBottom = (int) Math.floor(vp.get(1) + PanelCoords.screenY(py, vpY) * ky);
            int scW = Math.max(1, (int) Math.ceil(vpW * kx));
            int scH = Math.max(1, (int) Math.ceil(vpH * ky));
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scLeft, scBottom, scW, scH);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void quad(float x, float y, float w, float h) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    private static void loop(float x, float y, float w, float h, float inset) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x + inset, y + inset);
        GL11.glVertex2f(x + w - inset, y + inset);
        GL11.glVertex2f(x + w - inset, y + h - inset);
        GL11.glVertex2f(x + inset, y + h - inset);
        GL11.glEnd();
    }

    private static void color(Color c, float a) {
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, a);
    }

    // ================================================================== input

    @Override
    public void buttonPressed(Object id) {
        if (!(id instanceof String)) return;
        String s = (String) id;
        if (s.startsWith(BTN_TAB_PREFIX)) {
            try {
                switchTo(Tree.valueOf(s.substring(BTN_TAB_PREFIX.length())));
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        if (BTN_RESPEC.equals(s)) {
            if (model.getPending().isEmpty() && ownedCount() > 0) {
                // null = refused (paid respec without a story point).
                if (RespecRunner.run(currentTab) != null) SkillUISounds.playRespecReset();
                reloadModel();
            }
            return;
        }
        if (BTN_CONFIRM.equals(s)) {
            confirm();
        }
    }

    /** Commits the pending batch as one unit; flashes the failing node when it is refused. */
    private void confirm() {
        Set<String> pending = model.getPending();
        if (pending.isEmpty()) return;
        PurchasePlan.Result r = GameSpend.commit(currentTab, new ArrayList<String>(pending));
        if (r != null && r.success) {
            int tier = 0;
            for (String id : r.order) {
                Node n = nodesById.get(id);
                if (n != null) tier = Math.max(tier, n.getTier());
            }
            SkillUISounds.playTierSkillSound(currentTab, tier, 1f);
            reloadModel();
            return;
        }
        if (r != null && r.failedNodeId != null) flash(r.failedNodeId);
        Global.getLogger(SkillTreeView.class).warn("[SkillsOverhaul/UI] commit refused: "
                + (r == null ? "null result" : r.failedNodeId + " " + r.reason));
    }

    private void switchTo(Tree next) {
        if (next == null || next == currentTab) return;
        currentTab = next;
        buildTab();
    }

    private void flash(String nodeId) {
        flashes.put(nodeId, FLASH_SECONDS);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (events == null) return;
        float px = panel.getPosition().getX();
        float py = panel.getPosition().getY();

        for (InputEventAPI e : events) {
            if (e.isConsumed()) continue;

            if (e.isKeyDownEvent()) {
                int key = e.getEventValue();
                Tree[] tabs = Tree.values();
                for (int i = 0; i < tabs.length; i++) {
                    if (key == Keyboard.KEY_1 + i || key == Keyboard.KEY_NUMPAD1 + i) {
                        e.consume();
                        switchTo(tabs[i]);
                        return;
                    }
                }
                if (key == Keyboard.KEY_ESCAPE && !model.getPending().isEmpty()) {
                    e.consume();
                    reloadModel();
                    return;
                }
                continue;
            }

            if (!e.isMouseEvent() && !e.isMouseMoveEvent() && !e.isMouseScrollEvent()) continue;
            float sx = PanelCoords.cursorLocalX(e.getX(), px) - vpX;
            float sy = PanelCoords.cursorLocalY(e.getY(), py) - vpY;
            boolean inView = PanelCoords.contains(0f, 0f, vpW, vpH, sx, sy);

            if (e.isMouseScrollEvent()) {
                int v = e.getEventValue();
                if (inView && v != 0) {
                    layout.zoomBy(sx, sy, v > 0 ? WHEEL_ZOOM_STEP : 1.0 / WHEEL_ZOOM_STEP);
                    positionViewChildren();
                    e.consume();
                }
                continue;
            }

            if (e.isLMBDownEvent()) {
                if (inView) {
                    pressing = true;
                    dragging = false;
                    pressX = lastX = sx;
                    pressY = lastY = sy;
                    e.consume();
                }
                continue;
            }

            if (e.isMouseMoveEvent()) {
                // Mouse-move is never consumed: other UI needs it for hover.
                if (!pressing) continue;
                if (!Mouse.isButtonDown(0)) {
                    pressing = false;
                    dragging = false;
                    continue;
                }
                if (!dragging && Math.hypot(sx - pressX, sy - pressY) > DRAG_THRESHOLD) {
                    dragging = true;
                }
                if (dragging) {
                    layout.panBy(sx - lastX, sy - lastY);
                    positionViewChildren();
                }
                lastX = sx;
                lastY = sy;
                continue;
            }

            if (e.isLMBUpEvent() && pressing) {
                boolean wasDrag = dragging;
                pressing = false;
                dragging = false;
                e.consume();
                if (!wasDrag) {
                    String id = layout.hitTest(pressX, pressY);
                    if (id != null) handleNodeClick(id);
                }
            }
        }
    }

    private void handleNodeClick(String nodeId) {
        NodeStatus s = statuses.get(nodeId);
        if (s == null) return;
        if (s.state == NodeStatus.State.PENDING) {
            model.remove(nodeId);
            refreshState();
            return;
        }
        if (s.state == NodeStatus.State.OWNED) {
            toggleActiveSlot(nodeId);
            return;
        }
        SkillTreeModel.Rejection rej = model.add(nodeId);
        if (rej == null) {
            Node n = nodesById.get(nodeId);
            SkillUISounds.playTierSkillSound(currentTab, n == null ? 0 : n.getTier(), 0.55f);
            refreshState();
            return;
        }
        NodeStatus.Reason reason = rej.getReason();
        if (reason == NodeStatus.Reason.BUDGET || reason == NodeStatus.Reason.TIER) flash(nodeId);
    }

    /**
     * Owned T0 click: make it an active basic. With every slot full, v0 auto-displaces the
     * first active slot.
     */
    private void toggleActiveSlot(String nodeId) {
        Node clicked = nodesById.get(nodeId);
        if (clicked == null || clicked.getY() != 0) return;
        if (!ActiveLaneService.canSwapLive()) return; // In combat: ignore silently.
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (ActiveLaneService.isActive(mem, nodeId)) return;
        List<String> active = ActiveLaneService.getActive(mem);
        if (active.size() < ActiveLaneService.MAX_ACTIVE) {
            ActiveLaneService.add(mem, nodeId);
            NodeEffectApplier.reapplyForActiveChange(clicked, true);
            refreshState();
            return;
        }
        // TODO(active-basics): v0 auto-displaces the first active slot. A follow-up should
        //   open a modal letting the player choose which active T0 to displace.
        String displaced = active.get(0);
        if (ActiveLaneService.swap(mem, displaced, nodeId)) {
            TreeDef displacedDef = TreeRegistry.findTreeOfAnywhere(displaced);
            Node displacedNode = displacedDef != null ? displacedDef.getNode(displaced) : null;
            if (displacedNode != null) NodeEffectApplier.reapplyForActiveChange(displacedNode, false);
            NodeEffectApplier.reapplyForActiveChange(clicked, true);
            refreshState();
        }
    }

    // ================================================================== helpers

    private static boolean isState(NodeStatus s, NodeStatus.State state) {
        return s != null && s.state == state;
    }

    private static Color safeColor(Color fallback, boolean base) {
        try {
            return base ? Misc.getBasePlayerColor() : Misc.getDarkPlayerColor();
        } catch (Throwable t) {
            return fallback;
        }
    }

    /** "pilot_gunnery" -> "GUNNERY". */
    private static String prettyTreeName(String tree) {
        if (tree == null || tree.isEmpty()) return "";
        int underscore = tree.indexOf('_');
        String s = underscore >= 0 ? tree.substring(underscore + 1) : tree;
        return s.replace('_', ' ').toUpperCase();
    }
}
