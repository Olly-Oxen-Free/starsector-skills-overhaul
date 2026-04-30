package skills_overhaul.collective;

public class CombatMemoryTrackerTest {
    public static void main(String[] args) {
        CombatMemoryTracker tracker = new CombatMemoryTracker();

        assert tracker.getStacks("hegemony") == 0;

        tracker.recordEngagement("hegemony");
        assert tracker.getStacks("hegemony") == 1;

        tracker.recordEngagement("hegemony");
        tracker.recordEngagement("hegemony");
        tracker.recordEngagement("hegemony"); // past cap
        assert tracker.getStacks("hegemony") == 3 : "stacks should cap at 3";

        assert tracker.getStacks("luddic_church") == 0;
        tracker.recordEngagement("luddic_church");
        assert tracker.getStacks("luddic_church") == 1;

        tracker.recordEngagement("pirates");
        assert tracker.getStacks("hegemony") == 0 : "switch faction resets stacks";
        assert tracker.getStacks("luddic_church") == 0;
        assert tracker.getStacks("pirates") == 1;

        System.out.println("CombatMemoryTrackerTest PASSED");
    }
}
