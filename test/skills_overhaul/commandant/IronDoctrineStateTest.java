package skills_overhaul.commandant;

public class IronDoctrineStateTest {
    public static void main(String[] args) throws Exception {
        IronDoctrineState s = new IronDoctrineState();

        assert Math.abs(s.entrenchedMult(0) - 1.00f) < 1e-4f;
        assert Math.abs(s.entrenchedMult(1) - 1.05f) < 1e-4f;
        assert Math.abs(s.entrenchedMult(3) - 1.15f) < 1e-4f;
        assert Math.abs(s.entrenchedMult(5) - 1.25f) < 1e-4f;
        assert Math.abs(s.entrenchedMult(10) - 1.25f) < 1e-4f : "cap at 5 objectives";

        assert s.marchingActive(2, 1, false);
        assert !s.marchingActive(0, 2, false);
        assert s.marchingActive(0, 2, true) : "grace overrides";

        s.tickConsecutiveAllHeld(2f, true);
        assert s.consecutiveAllHeldSeconds() == 2f;
        s.tickConsecutiveAllHeld(3f, true);
        assert s.consecutiveAllHeldSeconds() == 5f;
        s.tickConsecutiveAllHeld(1f, false);
        assert s.consecutiveAllHeldSeconds() == 0f;

        s.tickConsecutiveAllHeld(9f, true);
        assert !s.totalControlActive();
        s.tickConsecutiveAllHeld(1f, true);
        assert s.totalControlActive();

        // Marching grace is measured in combat seconds (MARCHING_GRACE_SECONDS), not days.
        IronDoctrineState g = new IronDoctrineState();
        assert !g.isInMarchingGrace();
        g.tickMarchingGrace(1f, true);                       // holding majority tops grace to full 10s
        assert g.isInMarchingGrace();
        g.tickMarchingGrace(5f, false);                      // lost majority: 10 - 5 = 5s remaining
        assert g.isInMarchingGrace() : "within grace window";
        assert g.marchingActive(0, 2, g.isInMarchingGrace()) : "grace keeps marching active after majority lost";
        g.tickMarchingGrace(5f, false);                      // 5 - 5 = 0 → grace expires
        assert !g.isInMarchingGrace();
        assert !g.marchingActive(0, 2, g.isInMarchingGrace()) : "marching drops once grace expires";

        System.out.println("ALL PASSED");
    }
}
