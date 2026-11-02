package skills_overhaul.ui;

/**
 * Pure coordinate helpers for bottom-up panel-local space.
 *
 * Engine space has its origin at the bottom-left of the screen with y growing up.
 * {@code PositionAPI.getY()} is a component's BOTTOM edge and
 * {@code InputEventAPI.getY()} is in the same space. {@code inTL(x, yTop)} takes a
 * pad measured down from the parent's top edge, but the placed child's
 * {@code getY()} is still its bottom edge.
 *
 * Convention used by the Skills Overhaul panel:
 * <ul>
 *   <li>panel-local y is bottom-up: {@code localY = engineY - py} where {@code py} is
 *       the panel's {@code getY()};</li>
 *   <li>draw at {@code py + localY};</li>
 *   <li>an element placed with {@code inTL(x, yTop)} and height {@code h} has
 *       {@code localY = panelH - yTop - h} (its bottom edge);</li>
 *   <li>a cursor event has {@code localY = event.getY() - py}; no screen-height flip.</li>
 * </ul>
 *
 * No clamping: negative or out-of-panel results are returned as is.
 * Must not import {@code Global} or GL so it runs headless in tests.
 */
public final class PanelCoords {

    private PanelCoords() {}

    /** Bottom edge (bottom-up panel-local) of an element placed with {@code inTL(x, yTop)}. */
    public static float localYFromTop(float panelH, float yTop, float h) {
        return panelH - yTop - h;
    }

    /** Inverse of {@link #localYFromTop}: the {@code inTL} top pad for a bottom-up local y. */
    public static float topFromLocalY(float panelH, float localY, float h) {
        return panelH - localY - h;
    }

    /** Cursor x in panel-local space. */
    public static float cursorLocalX(float eventX, float px) {
        return eventX - px;
    }

    /** Cursor y in bottom-up panel-local space ({@code event.getY() - py}). */
    public static float cursorLocalY(float eventY, float py) {
        return eventY - py;
    }

    /** Engine-space x for a panel-local x. */
    public static float screenX(float px, float localX) {
        return px + localX;
    }

    /** Engine-space y for a bottom-up panel-local y ({@code py + localY}). */
    public static float screenY(float py, float localY) {
        return py + localY;
    }

    /**
     * Rect containment in one space (bottom-left corner {@code (rx, ry)}, size {@code rw x rh}).
     * Half-open: the left and bottom edges are inside, the right and top edges are outside.
     */
    public static boolean contains(float rx, float ry, float rw, float rh, float x, float y) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}
