package skills_overhaul.ui.layout;

import org.json.JSONObject;
import org.json.JSONTokener;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.TreeDef;
import skills_overhaul.tree.TreeLoader;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeLayoutTest {

    private static final String[] COMMANDER = {"artificer", "collective", "commandant", "corsair", "zealot"};
    private static final double[][] VIEWPORTS = {{1000, 700}, {600, 400}};

    public static void main(String[] args) throws Exception {
        File modRoot = findModRoot();
        List<TreeDef> all = new ArrayList<TreeDef>();
        for (String key : COMMANDER) {
            File path = new File(modRoot, "data/config/skills_overhaul/trees/commander_" + key + ".json");
            all.add(TreeLoader.fromJSON(new JSONObject(new JSONTokener(new FileReader(path)))));
        }

        for (double[] vp : VIEWPORTS) {
            // the whole tab as one layout, and each tree singly
            checkTab("all", all, vp[0], vp[1]);
            for (int i = 0; i < all.size(); i++) {
                checkTab(COMMANDER[i], Collections.singletonList(all.get(i)), vp[0], vp[1]);
            }
        }
        testEdges(all);
        testResize(all);
        testEmptyAndDegenerate(all);
        testOutOfRange();
        testNoStaticMutableFields();
        System.out.println("ALL PASSED");
    }

    // --- per-tab checks -----------------------------------------------------------

    private static void checkTab(String name, List<TreeDef> defs, double vw, double vh) {
        String tag = name + " @" + (int) vw + "x" + (int) vh;
        TreeLayout l = new TreeLayout(defs, vw, vh);
        checkGeometry(tag, l, defs);
        checkFit(tag, l);
        checkZoomClamp(tag, l);
        checkZoomAt(tag, l);
        checkPanClamp(tag, l);
        checkRoundTrip(tag, l);
        checkHitTest(tag, l);
    }

    private static void checkGeometry(String tag, TreeLayout l, List<TreeDef> defs) {
        int expected = 0;
        for (TreeDef d : defs) expected += d.getNodes().size();
        List<TreeLayout.NodeRect> ns = l.getNodes();
        assert ns.size() == expected : tag + ": laid out " + ns.size() + " of " + expected;
        double node = l.getNodeSize();
        assert node >= TreeLayout.DEFAULT_MIN_NODE && node <= TreeLayout.DEFAULT_MAX_NODE
                : tag + ": node size " + node;

        for (int i = 0; i < ns.size(); i++) {
            TreeLayout.Rect a = ns.get(i).rect;
            assert a.x >= 0 && a.y >= 0 && a.x + a.w <= l.getContentWidth() + 1e-9
                    && a.y + a.h <= l.getContentHeight() + 1e-9
                    : tag + ": " + ns.get(i).id + " " + a + " outside content "
                    + l.getContentWidth() + "x" + l.getContentHeight();
            for (int j = i + 1; j < ns.size(); j++) {
                TreeLayout.Rect b = ns.get(j).rect;
                boolean overlap = a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h;
                assert !overlap : tag + ": " + ns.get(i).id + " overlaps " + ns.get(j).id;
            }
        }
        // bottom-up: tier 0 lowest, capstone above it, centred in the column
        for (TreeDef d : defs) {
            TreeLayout.Rect t0 = l.getNodeRect(d.getT0().getId());
            TreeLayout.Rect t5 = l.getNodeRect(d.getT5().getId());
            assert t0.y < t5.y : tag + ": tier 0 not below capstone";
            assert Math.abs(t0.centerX() - t5.centerX()) < 1e-9 : tag + ": T0/capstone not aligned";
            for (Node n : d.getNodes()) {
                if (n.getTier() > 0) assert l.getNodeRect(n.getId()).y > t0.y
                        : tag + ": " + n.getId() + " not above T0";
            }
            TreeLayout.Rect col = l.getColumns().get(defs.indexOf(d));
            assert Math.abs(t0.centerX() - col.centerX()) < 1e-9 : tag + ": T0 not at column centre";
        }
    }

    private static void checkFit(String tag, TreeLayout l) {
        double z = l.getZoom();
        assert z == l.getFitZoom() : tag + ": default zoom is not fit";
        assert l.getPanX() == 0 && l.getPanY() == 0 : tag + ": default pan not zero";
        double[] lo = l.contentToScreen(0, 0);
        double[] hi = l.contentToScreen(l.getContentWidth(), l.getContentHeight());
        double eps = 1e-6;
        assert lo[0] >= -eps && lo[1] >= -eps : tag + ": content bottom-left off screen";
        assert hi[0] <= l.getViewportWidth() + eps && hi[1] <= l.getViewportHeight() + eps
                : tag + ": content top-right off screen";
        // fit is tight on at least one axis
        assert Math.abs(hi[0] - lo[0] - l.getViewportWidth()) < 1e-6
                || Math.abs(hi[1] - lo[1] - l.getViewportHeight()) < 1e-6 : tag + ": fit is not tight";
    }

    private static void checkZoomClamp(String tag, TreeLayout l) {
        double cx = l.getViewportWidth() / 2, cy = l.getViewportHeight() / 2;
        l.zoomAt(cx, cy, l.getFitZoom() * 0.1);
        assert l.getZoom() == l.getFitZoom() : tag + ": zoom below fit not clamped";
        assert l.getPanX() == 0 && l.getPanY() == 0 : tag + ": pan not zero at fit";
        l.zoomAt(cx, cy, 50);
        assert l.getZoom() == Math.max(l.getFitZoom(), 2.0) : tag + ": zoom above 2.0 not clamped";
        l.zoomBy(cx, cy, 10);
        assert l.getZoom() == Math.max(l.getFitZoom(), 2.0) : tag + ": zoomBy above 2.0 not clamped";
        l.resetView();
    }

    private static void checkZoomAt(String tag, TreeLayout l) {
        double vw = l.getViewportWidth(), vh = l.getViewportHeight();
        TreeLayout.Rect some = l.getNodes().get(l.getNodes().size() / 2).rect;
        double[] nodeScreen = l.contentToScreen(some.centerX(), some.centerY());
        double[][] cursors = {{vw / 2, vh / 2}, {0, 0}, {vw, vh}, {vw * 0.2, vh * 0.8}, nodeScreen};
        for (double[] c : cursors) {
            l.resetView();
            double f = l.getFitZoom();
            // two zoom-in steps from fit; each keeps the content point under the cursor fixed
            for (double t : new double[]{f * 1.5, f * 2.0}) {
                double[] before = l.screenToContent(c[0], c[1]);
                l.zoomAt(c[0], c[1], t);
                double[] s = l.contentToScreen(before[0], before[1]);
                assert Math.abs(s[0] - c[0]) < 0.5 && Math.abs(s[1] - c[1]) < 0.5
                        : tag + ": zoomAt(" + c[0] + "," + c[1] + "," + t + ") moved point to "
                        + s[0] + "," + s[1];
            }
        }
        // repeated wheel steps about an off-centre cursor keep the point fixed
        l.resetView();
        for (int i = 0; i < 10; i++) {
            double[] before = l.screenToContent(vw * 0.3, vh * 0.6);
            l.zoomBy(vw * 0.3, vh * 0.6, 1.1);
            double[] s = l.contentToScreen(before[0], before[1]);
            assert Math.abs(s[0] - vw * 0.3) < 0.5 && Math.abs(s[1] - vh * 0.6) < 0.5
                    : tag + ": wheel step " + i + " drifted";
        }
        l.resetView();
    }

    private static void checkPanClamp(String tag, TreeLayout l) {
        double vw = l.getViewportWidth(), vh = l.getViewportHeight();
        l.resetView();
        l.panBy(500, -300);
        assert l.getPanX() == 0 && l.getPanY() == 0 : tag + ": pan moved at fit zoom";
        l.zoomAt(vw / 2, vh / 2, 2.0);
        double[][] pushes = {{1e6, 0}, {-1e6, 0}, {0, 1e6}, {0, -1e6}, {1e6, 1e6}, {-1e6, -1e6}};
        for (double[] p : pushes) {
            l.panBy(p[0], p[1]);
            double[] lo = l.contentToScreen(0, 0);
            double[] hi = l.contentToScreen(l.getContentWidth(), l.getContentHeight());
            double visW = Math.min(hi[0], vw) - Math.max(lo[0], 0);
            double visH = Math.min(hi[1], vh) - Math.max(lo[1], 0);
            assert visW > 1 && visH > 1 : tag + ": content left the viewport after pan "
                    + p[0] + "," + p[1] + " (visible " + visW + "x" + visH + ")";
        }
        // the corner case: zoom in hard about a viewport corner, content must stay visible
        double[][] corners = {{0, 0}, {vw, vh}, {0, vh}, {vw, 0}};
        for (double[] c : corners) {
            l.resetView();
            l.zoomAt(c[0], c[1], 2.0);
            double[] lo = l.contentToScreen(0, 0);
            double[] hi = l.contentToScreen(l.getContentWidth(), l.getContentHeight());
            assert Math.min(hi[0], vw) - Math.max(lo[0], 0) > 1
                    && Math.min(hi[1], vh) - Math.max(lo[1], 0) > 1 : tag + ": corner zoom lost content";
        }
        // zooming back to fit recentres
        l.zoomAt(0, 0, l.getFitZoom());
        assert l.getPanX() == 0 && l.getPanY() == 0 : tag + ": pan not zero after zooming out to fit";
        l.resetView();
    }

    private static void checkRoundTrip(String tag, TreeLayout l) {
        double vw = l.getViewportWidth(), vh = l.getViewportHeight();
        double[][] states = {{1.0, 0, 0}, {1.3, 40, -25}, {2.0, -200, 120}, {1.8, 1e5, -1e5}};
        double[][] pts = {{0, 0}, {l.getContentWidth(), l.getContentHeight()}, {123.4, 56.7}, {-50, 900}};
        for (double[] st : states) {
            l.resetView();
            l.zoomAt(vw / 3, vh / 3, l.getFitZoom() * st[0]);
            l.panBy(st[1], st[2]);
            for (double[] p : pts) {
                double[] s = l.contentToScreen(p[0], p[1]);
                double[] back = l.screenToContent(s[0], s[1]);
                assert Math.abs(back[0] - p[0]) < 0.01 && Math.abs(back[1] - p[1]) < 0.01
                        : tag + ": round trip " + p[0] + "," + p[1] + " -> " + back[0] + "," + back[1];
            }
        }
        l.resetView();
    }

    private static void checkHitTest(String tag, TreeLayout l) {
        double vw = l.getViewportWidth(), vh = l.getViewportHeight();
        double[][] states = {{1.0, 0, 0}, {1.6, 60, -40}, {2.0, -150, 90}};
        for (double[] st : states) {
            l.resetView();
            l.zoomAt(vw * 0.6, vh * 0.4, l.getFitZoom() * st[0]);
            l.panBy(st[1], st[2]);
            int checked = 0;
            for (TreeLayout.NodeRect nr : l.getNodes()) {
                double[] s = l.contentToScreen(nr.rect.centerX(), nr.rect.centerY());
                if (s[0] < 0 || s[0] > vw || s[1] < 0 || s[1] > vh) continue;
                assert nr.id.equals(l.hitTest(s[0], s[1]))
                        : tag + ": hitTest at centre of " + nr.id + " gave " + l.hitTest(s[0], s[1]);
                checked++;
            }
            assert checked > 0 : tag + ": no node visible for hit test";
            // between two horizontally adjacent nodes of the same tier: the slot gap
            TreeLayout.NodeRect a = null, b = null;
            for (TreeLayout.NodeRect x : l.getNodes()) {
                for (TreeLayout.NodeRect y : l.getNodes()) {
                    if (x.treeIndex == y.treeIndex && x.tier == y.tier && y.slot == x.slot + 1) { a = x; b = y; }
                }
            }
            assert a != null : tag + ": no adjacent pair";
            double gx = (a.rect.x + a.rect.w + b.rect.x) / 2.0;
            double[] s = l.contentToScreen(gx, a.rect.centerY());
            assert l.hitTest(s[0], s[1]) == null : tag + ": hit between " + a.id + " and " + b.id;
            // between tiers
            double[] s2 = l.contentToScreen(a.rect.centerX(), a.rect.y - 1.0);
            assert l.hitTest(s2[0], s2[1]) == null || a.tier == 0 : tag + ": hit below " + a.id;
        }
        l.resetView();
    }

    // --- other cases ----------------------------------------------------------------

    private static void testEdges(List<TreeDef> all) {
        TreeLayout l = new TreeLayout(all, 1000, 700);
        int expected = 0;
        for (TreeDef d : all) for (Node n : d.getNodes()) expected += n.getReferencedPrereqIds().size();
        assert l.getEdges().size() == expected : "edges " + l.getEdges().size() + " != " + expected;
        for (TreeLayout.Edge e : l.getEdges()) {
            TreeLayout.Rect from = l.getNodeRect(e.fromId), to = l.getNodeRect(e.toId);
            assert e.x1 == from.centerX() && e.y1 == from.centerY() : "edge start not at parent centre";
            assert e.x2 == to.centerX() && e.y2 == to.centerY() : "edge end not at child centre";
        }
    }

    private static void testResize(List<TreeDef> all) {
        TreeLayout l = new TreeLayout(all, 1000, 700);
        l.zoomAt(500, 350, l.getFitZoom() * 1.5);
        l.panBy(80, 40);
        l.resize(600, 400);
        assert l.getViewportWidth() == 600 && l.getViewportHeight() == 400 : "resize not applied";
        assert Math.abs(l.getZoom() / l.getFitZoom() - 1.5) < 1e-9 : "resize lost relative zoom";
        TreeLayout fresh = new TreeLayout(all, 600, 400);
        assert Math.abs(fresh.getFitZoom() - l.getFitZoom()) < 1e-9 : "resize fit differs from fresh";
        l.resize(600, 400);
        l.zoomAt(0, 0, l.getFitZoom());
        assert l.getPanX() == 0 && l.getPanY() == 0 : "pan not zero at fit after resize";
    }

    private static void testEmptyAndDegenerate(List<TreeDef> all) {
        TreeLayout e = new TreeLayout(new ArrayList<TreeDef>(), 1000, 700);
        assert e.getContentWidth() == 0 && e.getContentHeight() == 0 : "empty content not zero";
        assert e.getNodes().isEmpty() && e.getEdges().isEmpty();
        assert e.hitTest(10, 10) == null;
        e.zoomAt(5, 5, 1.5); e.panBy(10, 10); e.resize(0, 0);
        double[] p = e.screenToContent(1, 1);
        assert !Double.isNaN(p[0]) && !Double.isInfinite(p[0]) : "empty transform not finite";

        TreeLayout n = new TreeLayout(null, 1000, 700);
        assert n.getNodes().isEmpty() && n.hitTest(0, 0) == null;

        // zero and negative viewports must not divide by zero
        for (double[] vp : new double[][]{{0, 0}, {-10, -10}, {0, 500}}) {
            TreeLayout z = new TreeLayout(all, vp[0], vp[1]);
            assert z.getZoom() > 0 && !Double.isInfinite(z.getZoom()) && !Double.isNaN(z.getZoom())
                    : "bad zoom for viewport " + vp[0] + "x" + vp[1];
            double[] q = z.screenToContent(0, 0);
            assert !Double.isNaN(q[0]) && !Double.isInfinite(q[0]) && !Double.isNaN(q[1]);
            z.hitTest(0, 0);
            z.zoomAt(0, 0, 3); z.panBy(5, 5);
        }

        // tiny viewport: node size bound holds, fit zoom drops below 1
        TreeLayout t = new TreeLayout(all, 200, 100);
        assert t.getNodeSize() == TreeLayout.DEFAULT_MIN_NODE : "node size below minimum";
        assert t.getFitZoom() < 1.0 : "fit zoom should drop below 1";
        double[] hi = t.contentToScreen(t.getContentWidth(), t.getContentHeight());
        assert hi[0] <= 200 + 1e-6 && hi[1] <= 100 + 1e-6 : "tiny viewport does not fit";

        // explicit node bounds are honoured
        TreeLayout big = new TreeLayout(Collections.singletonList(all.get(0)), 4000, 4000, 50, 80);
        assert big.getNodeSize() == 80 : "max node bound not honoured: " + big.getNodeSize();
        TreeLayout small = new TreeLayout(all, 300, 300, 50, 80);
        assert small.getNodeSize() == 50 : "min node bound not honoured: " + small.getNodeSize();
    }

    private static void testOutOfRange() throws Exception {
        Map<String, Node> byId = new HashMap<String, Node>();
        byId.put("base", node("base", 2, 0, null));
        byId.put("wide", node("wide", 9, 2, "base"));      // slot past 4 -> clamped to 4
        byId.put("neg", node("neg", -3, 1, "base"));       // slot below 0 -> clamped to 0
        byId.put("tall", node("tall", 2, 7, "base"));      // tier above 5 -> extra rows
        byId.put("ghost", node("ghost", 1, 1, "missing")); // prereq not in the tab -> no edge
        TreeDef d = new TreeDef("odd", byId);
        TreeLayout l = new TreeLayout(Collections.singletonList(d), 800, 600);
        assert l.getNodes().size() == 5;
        for (TreeLayout.NodeRect nr : l.getNodes()) {
            TreeLayout.Rect r = nr.rect;
            assert r.x >= 0 && r.y >= 0 && r.x + r.w <= l.getContentWidth() + 1e-9
                    && r.y + r.h <= l.getContentHeight() + 1e-9 : nr.id + " outside content";
        }
        TreeLayout.Rect col = l.getColumns().get(0);
        assert l.getNodeRect("wide").x + l.getNodeRect("wide").w <= col.x + col.w;
        assert l.getNodeRect("tall").y > l.getNodeRect("base").y;
        assert l.getEdges().size() == 3 : "edges " + l.getEdges().size();
    }

    private static Node node(String id, int x, int y, String prereq) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("name", id); o.put("desc", ""); o.put("x", x); o.put("y", y); o.put("cost", 1);
        if (prereq != null) o.put("prereqs", new org.json.JSONArray().put(
                new JSONObject().put("all", new org.json.JSONArray().put(prereq))));
        return Node.fromJSON(o);
    }

    private static void testNoStaticMutableFields() {
        for (Class<?> c : new Class<?>[]{TreeLayout.class, TreeLayout.Rect.class,
                TreeLayout.NodeRect.class, TreeLayout.Edge.class}) {
            for (Field f : c.getDeclaredFields()) {
                int m = f.getModifiers();
                if (Modifier.isStatic(m)) assert Modifier.isFinal(m) : c.getSimpleName() + "." + f.getName()
                        + " is static and mutable";
            }
        }
    }

    /** Walk up from cwd looking for mod_info.json — handles tests run from jars/classes. */
    private static File findModRoot() {
        File d = new File(".").getAbsoluteFile();
        for (int i = 0; i < 8 && d != null; i++) {
            if (new File(d, "mod_info.json").exists()) return d;
            d = d.getParentFile();
        }
        throw new IllegalStateException("could not locate mod root (mod_info.json) from "
                + new File(".").getAbsolutePath());
    }
}
