package skills_overhaul.ui.layout;

import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure layout, zoom and pan state for one skills tab (a list of {@link TreeDef}s).
 *
 * <h3>Spaces</h3>
 * <ul>
 *   <li><b>Content space</b>: bottom-up, origin at the bottom-left of the laid-out content,
 *       y grows up. Tier 0 has the lowest y. Units are px at zoom 1.</li>
 *   <li><b>Screen space</b>: bottom-up and viewport-local, origin at the viewport's
 *       bottom-left (the {@code PositionAPI.getX()/getY()} corner), extent
 *       {@code [0, viewportW] x [0, viewportH]}.</li>
 * </ul>
 * The transform is {@code screen = offset + zoom * content}; a view applies it with
 * {@code glTranslatef(viewportX + getOffsetX(), viewportY + getOffsetY(), 0)} followed by
 * {@code glScalef(getZoom(), getZoom(), 1)}.
 *
 * <h3>Geometry</h3>
 * Each TreeDef is one column, laid out left to right in list order. Inside a column a node
 * sits at {@code x = colX + innerPad + slot * (node + slotGap)} and
 * {@code y = bottomPad + tier * (node + tierGap)}. Slot 2 is the column centre, so T0 and
 * the capstone (slot 2) land centred. Spacing follows the ratios of the old
 * {@code PathTreeBuilder}; columns are separated by {@code 2 * laneGap}. A header band of
 * {@link #getHeaderHeight()} sits above the top tier of every column.
 * <p>
 * Out-of-range input is laid out, never rejected: slots are clamped to 0..4, negative tiers
 * to 0, and tiers above 5 add rows above the capstone row. A clamped node may then overlap a
 * valid node at the same position; the tree JSON, not this class, enforces the bounds.
 *
 * <h3>Node size</h3>
 * The node size is the largest value in {@code [minNode, maxNode]} whose content fits the
 * viewport at zoom 1. When even {@code minNode} does not fit, the node size stays at
 * {@code minNode} and the fit zoom drops below 1.
 *
 * <h3>View state</h3>
 * Zoom is clamped to {@code [fitZoom, max(fitZoom, 2.0)]}. Pan is the offset relative to the
 * centred position and is clamped per axis to {@code |pan| <= min((v/2)(zoom/fit - 1),
 * (zoom*c + v)/2 - keep)}, where {@code keep = min(zoom*c, v)/4}. The first term is zero at
 * fit zoom (so pan is zero there) and admits every pan that a zoom-in about a point inside
 * the viewport produces, so {@link #zoomAt} keeps the point under the cursor fixed; the
 * second term keeps at least a quarter of the content (or of the viewport) visible, so the
 * content never leaves the viewport entirely. Zooming out shrinks the range and recentres.
 * <p>
 * No Global, no GL and no static mutable state: every instance is independent.
 */
public final class TreeLayout {

    /** Default node size bounds, matching the old auto-scaling range. */
    public static final double DEFAULT_MIN_NODE = 44.0;
    public static final double DEFAULT_MAX_NODE = 120.0;
    /** Upper zoom limit (unless the fit zoom is already above it). */
    public static final double MAX_ZOOM = 2.0;

    private static final int SLOTS = 5;
    private static final int MIN_ROWS = 6;          // tiers 0..5
    private static final double KEEP_VISIBLE_FRAC = 0.25;

    /** Axis-aligned rectangle in content space; {@code (x, y)} is the bottom-left corner. */
    public static final class Rect {
        public final double x, y, w, h;
        Rect(double x, double y, double w, double h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        public double centerX() { return x + w / 2.0; }
        public double centerY() { return y + h / 2.0; }
        /** Inclusive of the edges. */
        public boolean contains(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
        @Override public String toString() { return "Rect(" + x + "," + y + " " + w + "x" + h + ")"; }
    }

    /** One laid-out node. */
    public static final class NodeRect {
        public final String id;
        public final int treeIndex;
        public final int tier, slot;      // as laid out (after clamping)
        public final Rect rect;
        NodeRect(String id, int treeIndex, int tier, int slot, Rect rect) {
            this.id = id; this.treeIndex = treeIndex; this.tier = tier; this.slot = slot; this.rect = rect;
        }
    }

    /** One prerequisite edge, from the parent (prerequisite) centre to the child centre. */
    public static final class Edge {
        public final String fromId, toId;
        public final double x1, y1, x2, y2;
        Edge(String fromId, String toId, double x1, double y1, double x2, double y2) {
            this.fromId = fromId; this.toId = toId; this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }
    }

    // --- Inputs --------------------------------------------------------------
    private final List<TreeDef> defs;
    private final double minNode, maxNode;
    private double viewportW, viewportH;

    // --- Geometry (recomputed by layout()) -----------------------------------
    private double node, slotGap, laneGap, innerPad, tierGap, headerH, topPad;
    private double contentW, contentH;
    private int rows;
    private final List<NodeRect> nodes = new ArrayList<NodeRect>();
    private final List<Edge> edges = new ArrayList<Edge>();
    private final List<Rect> columns = new ArrayList<Rect>();
    private final Map<String, NodeRect> byId = new HashMap<String, NodeRect>();

    // --- View state ------------------------------------------------------------
    private double fitZoom = 1.0;
    private double zoom = 1.0;
    private double panX, panY;

    public TreeLayout(List<TreeDef> defs, double viewportW, double viewportH) {
        this(defs, viewportW, viewportH, DEFAULT_MIN_NODE, DEFAULT_MAX_NODE);
    }

    /**
     * @param defs     the tab's trees, one column each; null is treated as empty
     * @param minNode  smallest node size in px; non-positive falls back to the default
     * @param maxNode  largest node size in px; below {@code minNode} it is raised to it
     */
    public TreeLayout(List<TreeDef> defs, double viewportW, double viewportH,
                      double minNode, double maxNode) {
        List<TreeDef> copy = new ArrayList<TreeDef>();
        if (defs != null) for (TreeDef d : defs) if (d != null) copy.add(d);
        this.defs = Collections.unmodifiableList(copy);
        this.minNode = minNode > 0 ? minNode : DEFAULT_MIN_NODE;
        this.maxNode = Math.max(this.minNode, maxNode > 0 ? maxNode : DEFAULT_MAX_NODE);
        this.viewportW = sanitize(viewportW);
        this.viewportH = sanitize(viewportH);
        layout();
        resetView();
    }

    // --- Layout ----------------------------------------------------------------

    /**
     * Change the viewport size: picks a new node size, refits, keeps the zoom relative to
     * fit (clamped) and re-clamps the pan.
     */
    public void resize(double newViewportW, double newViewportH) {
        double ratio = fitZoom > 0 ? zoom / fitZoom : 1.0;
        viewportW = sanitize(newViewportW);
        viewportH = sanitize(newViewportH);
        layout();
        fitZoom = computeFitZoom();
        zoom = clampZoom(fitZoom * ratio);
        clampPan();
    }

    private void layout() {
        int maxTier = 0;
        for (TreeDef d : defs) for (Node n : d.getNodes()) maxTier = Math.max(maxTier, n.getTier());
        rows = Math.max(MIN_ROWS, maxTier + 1);

        node = chooseNodeSize();
        applySpacing(node);

        nodes.clear(); edges.clear(); columns.clear(); byId.clear();
        int cols = defs.size();
        if (cols == 0) { contentW = 0; contentH = 0; return; }

        contentW = widthFor(node, cols);
        contentH = heightFor(node);
        double colW = columnWidth();
        for (int c = 0; c < cols; c++) {
            double colX = c * (colW + 2.0 * laneGap);
            columns.add(new Rect(colX, 0, colW, contentH));
            for (Node n : defs.get(c).getNodes()) {
                int slot = clampInt(n.getSlot(), 0, SLOTS - 1);
                int tier = Math.max(0, n.getTier());
                double x = colX + innerPad + slot * (node + slotGap);
                double y = innerPad + tier * (node + tierGap);
                NodeRect nr = new NodeRect(n.getId(), c, tier, slot, new Rect(x, y, node, node));
                nodes.add(nr);
                if (!byId.containsKey(n.getId())) byId.put(n.getId(), nr);
            }
        }
        // Edges: resolve a prereq in the node's own tree first, then anywhere in the tab.
        Map<String, NodeRect> local = new HashMap<String, NodeRect>();
        for (int c = 0; c < cols; c++) {
            local.clear();
            for (NodeRect nr : nodes) if (nr.treeIndex == c) local.put(nr.id, nr);
            for (Node child : defs.get(c).getNodes()) {
                NodeRect to = local.get(child.getId());
                for (String ref : child.getReferencedPrereqIds()) {
                    NodeRect from = local.containsKey(ref) ? local.get(ref) : byId.get(ref);
                    if (from == null || to == null) continue;
                    edges.add(new Edge(from.id, to.id, from.rect.centerX(), from.rect.centerY(),
                            to.rect.centerX(), to.rect.centerY()));
                }
            }
        }
    }

    /** Largest node size in [minNode, maxNode] that fits the viewport at zoom 1. */
    private double chooseNodeSize() {
        int cols = Math.max(1, defs.size());
        if (fits(maxNode, cols)) return maxNode;
        if (!fits(minNode, cols)) return minNode;
        double lo = minNode, hi = maxNode;      // fits(lo) && !fits(hi); sizes are monotone
        for (int i = 0; i < 40; i++) {
            double mid = (lo + hi) / 2.0;
            if (fits(mid, cols)) lo = mid; else hi = mid;
        }
        return lo;
    }

    private boolean fits(double n, int cols) {
        return widthFor(n, cols) <= viewportW && heightFor(n) <= viewportH;
    }

    private void applySpacing(double n) {
        slotGap  = clamp(n * 0.18, 6, 22);
        laneGap  = clamp(n * 0.40, 16, 52);
        innerPad = clamp(n * 0.20, 8, 28);
        tierGap  = clamp(n * 0.30, 12, 22);
        headerH  = clamp(n * 0.45, 22, 32);
        topPad   = clamp(n * 0.14, 4, 10);
    }

    private double columnWidth() { return columnWidth(node); }

    // Pure size functions of a node size n, using the same ratios as applySpacing.
    private static double columnWidth(double n) {
        return SLOTS * n + (SLOTS - 1) * clamp(n * 0.18, 6, 22) + 2.0 * clamp(n * 0.20, 8, 28);
    }

    private static double widthFor(double n, int cols) {
        return cols * columnWidth(n) + Math.max(0, cols - 1) * 2.0 * clamp(n * 0.40, 16, 52);
    }

    private double heightFor(double n) {
        return clamp(n * 0.20, 8, 28) + rows * n + (rows - 1) * clamp(n * 0.30, 12, 22)
                + clamp(n * 0.14, 4, 10) + clamp(n * 0.45, 22, 32);
    }

    // --- Geometry accessors -------------------------------------------------------

    public List<TreeDef> getTreeDefs()  { return defs; }
    public double getNodeSize()         { return node; }
    public double getHeaderHeight()     { return headerH; }
    public double getContentWidth()     { return contentW; }
    public double getContentHeight()    { return contentH; }
    public double getViewportWidth()    { return viewportW; }
    public double getViewportHeight()   { return viewportH; }
    public List<NodeRect> getNodes()    { return Collections.unmodifiableList(nodes); }
    public List<Edge> getEdges()        { return Collections.unmodifiableList(edges); }
    /** Full-height column rect for each TreeDef, in list order; the header is its top band. */
    public List<Rect> getColumns()      { return Collections.unmodifiableList(columns); }

    /** Content-space rect of the node with this id (the first one, if ids repeat), or null. */
    public Rect getNodeRect(String id) {
        NodeRect nr = byId.get(id);
        return nr == null ? null : nr.rect;
    }

    // --- View state -------------------------------------------------------------

    public double getZoom()    { return zoom; }
    public double getFitZoom() { return fitZoom; }
    public double getMaxZoom() { return Math.max(fitZoom, MAX_ZOOM); }
    public double getPanX()    { return panX; }
    public double getPanY()    { return panY; }
    /** Screen x of content x = 0. */
    public double getOffsetX() { return (viewportW - zoom * contentW) / 2.0 + panX; }
    /** Screen y of content y = 0. */
    public double getOffsetY() { return (viewportH - zoom * contentH) / 2.0 + panY; }

    /** Zoom to fit, centred. */
    public void resetView() {
        fitZoom = computeFitZoom();
        zoom = fitZoom;
        panX = 0; panY = 0;
    }

    /**
     * Zoom to {@code targetZoom} (clamped to [fit, max]) keeping the content point under the
     * screen point (cx, cy) fixed, then clamp the pan. The point stays fixed when zooming in
     * about a point inside the viewport, unless that would push the content out of view.
     */
    public void zoomAt(double cx, double cy, double targetZoom) {
        double[] p = screenToContent(cx, cy);
        zoom = clampZoom(targetZoom);
        panX = cx - zoom * p[0] - (viewportW - zoom * contentW) / 2.0;
        panY = cy - zoom * p[1] - (viewportH - zoom * contentH) / 2.0;
        clampPan();
    }

    /** {@link #zoomAt} with {@code zoom * factor}; a wheel step passes e.g. 1.1 or 1/1.1. */
    public void zoomBy(double cx, double cy, double factor) {
        if (!(factor > 0)) return;
        zoomAt(cx, cy, zoom * factor);
    }

    /** Move the content by (dx, dy) screen px, clamped. */
    public void panBy(double dx, double dy) {
        panX += dx; panY += dy;
        clampPan();
    }

    /** Set the pan (offset from the centred position, screen px), clamped. */
    public void setPan(double px, double py) {
        panX = px; panY = py;
        clampPan();
    }

    // --- Transform ------------------------------------------------------------------

    public double[] contentToScreen(double x, double y) {
        return new double[]{getOffsetX() + zoom * x, getOffsetY() + zoom * y};
    }

    public double[] screenToContent(double sx, double sy) {
        return new double[]{(sx - getOffsetX()) / zoom, (sy - getOffsetY()) / zoom};
    }

    /** Id of the node whose rect contains the screen point, or null. */
    public String hitTest(double sx, double sy) {
        double[] p = screenToContent(sx, sy);
        for (NodeRect nr : nodes) if (nr.rect.contains(p[0], p[1])) return nr.id;
        return null;
    }

    // --- Clamps -----------------------------------------------------------------------

    private double computeFitZoom() {
        if (contentW <= 0 || contentH <= 0 || viewportW <= 0 || viewportH <= 0) return 1.0;
        return Math.min(viewportW / contentW, viewportH / contentH);
    }

    private double clampZoom(double z) {
        if (Double.isNaN(z)) return zoom;
        return clamp(z, fitZoom, getMaxZoom());
    }

    private void clampPan() {
        double rx = panRange(viewportW, contentW);
        double ry = panRange(viewportH, contentH);
        panX = clamp(panX, -rx, rx);
        panY = clamp(panY, -ry, ry);
    }

    /** Largest |pan| allowed on one axis; see the class comment. */
    private double panRange(double v, double c) {
        if (fitZoom <= 0 || c <= 0) return 0;
        double zc = zoom * c;
        double zoomTerm = (v / 2.0) * (zoom / fitZoom - 1.0);
        double keepTerm = (zc + v) / 2.0 - KEEP_VISIBLE_FRAC * Math.min(zc, v);
        return Math.max(0, Math.min(zoomTerm, keepTerm));
    }

    // --- Pure helpers -------------------------------------------------------------------

    private static double sanitize(double v) {
        return (Double.isNaN(v) || v < 0) ? 0 : v;
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
