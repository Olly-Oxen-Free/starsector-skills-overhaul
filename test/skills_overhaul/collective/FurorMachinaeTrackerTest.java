package skills_overhaul.collective;

public class FurorMachinaeTrackerTest {
    public static void main(String[] args) {
        SOFurorMachinaeTracker tracker = new SOFurorMachinaeTracker();

        // test 1: initial stacks are 0
        assert tracker.getStacks("ship1") == 0f : "Initial stacks should be 0";

        // test 2: record hit adds weight
        tracker.recordHit("ship1", 1f);
        assert tracker.getStacks("ship1") == 1f : "Expected 1 stack";

        // test 3: stacks capped at MAX_STACKS
        for (int i = 0; i < 20; i++) tracker.recordHit("ship1", 1f);
        assert tracker.getStacks("ship1") == SOFurorMachinaeTracker.MAX_STACKS
                : "Expected max stacks " + SOFurorMachinaeTracker.MAX_STACKS;

        // test 4: advance decays stacks
        tracker.advance("ship1", 1f);
        assert Math.abs(tracker.getStacks("ship1") - (SOFurorMachinaeTracker.MAX_STACKS - 1f)) < 0.001f
                : "Expected " + (SOFurorMachinaeTracker.MAX_STACKS - 1f) + " after 1s decay";

        // test 5: advance clamps to 0
        tracker.advance("ship1", 1000f);
        assert tracker.getStacks("ship1") == 0f : "Stacks should clamp to 0";

        // test 6: shield hits add half weight
        tracker.recordHit("ship2", 0.5f);
        assert tracker.getStacks("ship2") == 0.5f : "Expected 0.5 stacks from shield hit";

        System.out.println("FurorMachinaeTrackerTest: all passed.");
    }
}
