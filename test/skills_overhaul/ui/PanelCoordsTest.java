package skills_overhaul.ui;

public class PanelCoordsTest {

    /** Pre-fix cursor conversion: top-down local y via a screen-height flip. */
    private static float legacyTopDownY(float screenH, float eventY, float py) {
        return (screenH - eventY) - py;
    }

    public static void main(String[] args) throws Exception {
        float screenH = 1080f;
        float panelH = 600f;
        float py = 100f;               // non-centred: screenH/2 - panelH/2 would be 240
        assert py != screenH / 2f - panelH / 2f : "fixture must be non-centred";

        // inTL conversion: localY = panelH - yTop - h
        assert PanelCoords.localYFromTop(panelH, 14f, 30f) == panelH - 14f - 30f : "inTL(14) h30";
        assert PanelCoords.localYFromTop(panelH, 0f, 30f) == panelH - 30f : "yTop=0";
        assert PanelCoords.localYFromTop(panelH, 14f, 0f) == panelH - 14f : "h=0";
        assert PanelCoords.localYFromTop(panelH, 590f, 30f) == -20f : "negative result is not clamped";
        assert PanelCoords.localYFromTop(panelH, 0f, 800f) == -200f : "element taller than panel";
        assert PanelCoords.topFromLocalY(panelH, PanelCoords.localYFromTop(panelH, 37f, 22f), 22f) == 37f
                : "topFromLocalY inverts localYFromTop";

        // Cursor conversion: event.getY() - py, no screen-height flip.
        assert PanelCoords.cursorLocalY(250f, py) == 150f : "cursor y 250 -> 150";
        assert PanelCoords.cursorLocalY(50f, py) == -50f : "cursor below panel is negative";
        assert PanelCoords.cursorLocalX(420f, 20f) == 400f : "cursor x";
        assert PanelCoords.screenY(py, 150f) == 250f : "screenY inverts cursorLocalY";
        assert PanelCoords.screenX(20f, 400f) == 420f : "screenX";

        // Rect containment: bottom/left inclusive, top/right exclusive.
        float rx = 10f, ry = PanelCoords.localYFromTop(panelH, 14f, 30f), rw = 40f, rh = 30f;
        assert PanelCoords.contains(rx, ry, rw, rh, 20f, ry + 5f) : "inside";
        assert PanelCoords.contains(rx, ry, rw, rh, rx, ry) : "bottom-left corner is inside";
        assert !PanelCoords.contains(rx, ry, rw, rh, 20f, ry - 0.5f) : "just below bottom edge";
        assert !PanelCoords.contains(rx, ry, rw, rh, 20f, ry + rh) : "top edge is outside";
        assert !PanelCoords.contains(rx, ry, rw, rh, rx - 0.5f, ry + 5f) : "just left of left edge";
        assert !PanelCoords.contains(rx, ry, rw, rh, rx + rw, ry + 5f) : "right edge is outside";
        assert !PanelCoords.contains(rx, ry, 0f, 0f, rx, ry) : "empty rect contains nothing";

        // A cursor over an inTL-placed rect hits it in the non-centred panel.
        float eventY = PanelCoords.screenY(py, ry + 10f);
        assert PanelCoords.contains(rx, ry, rw, rh, 20f, PanelCoords.cursorLocalY(eventY, py))
                : "event inside inTL rect (non-centred)";

        // Old formula is right only for a centred panel; the new one is right for both.
        float centredPy = screenH / 2f - panelH / 2f;
        float localTrue = 150f;
        float evCentred = centredPy + localTrue;
        float evOff = py + localTrue;
        float legacyCentred = panelH - legacyTopDownY(screenH, evCentred, centredPy);
        float legacyOff = panelH - legacyTopDownY(screenH, evOff, py);
        assert legacyCentred == localTrue : "legacy formula holds when centred";
        assert legacyOff != localTrue : "legacy formula is wrong when not centred";
        assert PanelCoords.cursorLocalY(evCentred, centredPy) == localTrue : "new formula, centred";
        assert PanelCoords.cursorLocalY(evOff, py) == localTrue : "new formula, non-centred";

        System.out.println("ALL PASSED");
    }
}
