package skills_overhaul.ui;

import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.*;

/**
 * Draws prereq edges between node centers via GL_LINES. State-dependent colors:
 *   - both endpoints owned → teal
 *   - child pending       → amber
 *   - either unmet        → gray
 *   - cross-lane hybrid   → stippled green
 *
 * Coordinates are panel-local pixels; caller must ensure the GL matrix is in the
 * right space before calling draw().
 */
public final class ConnectionLineDrawer {

    private ConnectionLineDrawer() {}

    public static final class Edge {
        public final float x1, y1, x2, y2;
        public final Color color;
        public final boolean stippled;
        public final boolean active;
        public Edge(float x1, float y1, float x2, float y2, Color color, boolean stippled, boolean active) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.color = color; this.stippled = stippled;
            this.active = active;
        }
    }

    /**
     * @param offsetX screen-space X to add to each edge endpoint
     * @param offsetY anchor Y (GL screen-space) used as the y reference; if
     *                {@code flipY} is true, the edge's top-down y is
     *                subtracted from this value (so offsetY should be the
     *                GL y of the panel TOP, i.e. py + panelHeight).
     * @param flipY when true, treat edge (x,y) as panel-local top-down and
     *              convert with {@code offsetY - y} (legacy y-up drawing).
     *              When false, use {@code offsetX + x, offsetY + y} directly
     *              (panel-local top-down + screen top-left = Starsector UI space).
     */
    public static void draw(List<Edge> edges, float alphaMult,
                            float offsetX, float offsetY, boolean flipY) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.5f);

        for (Edge e : edges) {
            if (e.stippled) {
                GL11.glEnable(GL11.GL_LINE_STIPPLE);
                GL11.glLineStipple(2, (short) 0x3333);
            } else if (!e.active) {
                GL11.glEnable(GL11.GL_LINE_STIPPLE);
                GL11.glLineStipple(1, (short) 0x1111);
            } else {
                GL11.glDisable(GL11.GL_LINE_STIPPLE);
            }
            float a = (e.color.getAlpha() / 255f) * alphaMult;
            GL11.glColor4f(e.color.getRed()/255f, e.color.getGreen()/255f, e.color.getBlue()/255f, a);
            GL11.glBegin(GL11.GL_LINES);
            if (flipY) {
                GL11.glVertex2f(offsetX + e.x1, offsetY - e.y1);
                GL11.glVertex2f(offsetX + e.x2, offsetY - e.y2);
            } else {
                GL11.glVertex2f(offsetX + e.x1, offsetY + e.y1);
                GL11.glVertex2f(offsetX + e.x2, offsetY + e.y2);
            }
            GL11.glEnd();
        }

        GL11.glPopAttrib();
    }

    /** @deprecated pre-flip signature retained for compatibility. */
    @Deprecated
    public static void draw(List<Edge> edges, float alphaMult, float offsetX, float offsetY) {
        draw(edges, alphaMult, offsetX, offsetY, false);
    }
}
