package skills_overhaul.pilot.maneuvering;

public class PerfectTimingTrackerTest {
    public static void main(String[] args) {
        PerfectTimingTracker t = new PerfectTimingTracker();

        assert !t.isBuffActive();

        t.advance(5f, false);
        assert !t.isBuffActive();

        t.advance(1f, false);
        assert t.isBuffActive();

        t.advance(0.1f, true);
        assert !t.isBuffActive();

        t.advance(5.9f, false);
        assert !t.isBuffActive();
        t.advance(0.1f, false);
        assert t.isBuffActive();

        System.out.println("ALL PASSED");
    }
}
