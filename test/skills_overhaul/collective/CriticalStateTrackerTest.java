package skills_overhaul.collective;

public class CriticalStateTrackerTest {
    public static void main(String[] args) {
        SOCriticalStateTracker tracker = new SOCriticalStateTracker();

        // test 1: not triggered initially
        assert !tracker.hasTriggered("ship1") : "Should not be triggered initially";

        // test 2: mark triggered
        tracker.markTriggered("ship1");
        assert tracker.hasTriggered("ship1") : "Should be triggered after markTriggered";

        // test 3: other ship not affected
        assert !tracker.hasTriggered("ship2") : "ship2 should not be triggered";

        // test 4: reset clears state
        tracker.reset();
        assert !tracker.hasTriggered("ship1") : "Should not be triggered after reset";

        System.out.println("CriticalStateTrackerTest: all passed.");
    }
}
