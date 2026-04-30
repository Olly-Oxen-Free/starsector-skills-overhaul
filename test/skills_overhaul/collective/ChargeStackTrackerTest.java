package skills_overhaul.collective;

public class ChargeStackTrackerTest {

    public static void main(String[] args) {
        // test 1: accumulate
        ChargeStackTracker t = new ChargeStackTracker();
        t.addStack("a:b"); t.addStack("a:b"); t.addStack("a:b");
        assert t.getStacks("a:b") == 3 : "Expected 3";

        // test 2: detonation at 15
        ChargeStackTracker t2 = new ChargeStackTracker();
        for (int i = 0; i < 14; i++) t2.addStack("a:b");
        assert !t2.checkAndConsumeDetonation("a:b") : "Should not detonate at 14";
        t2.addStack("a:b");
        assert t2.checkAndConsumeDetonation("a:b") : "Should detonate at 15";
        assert t2.getStacks("a:b") == 0 : "Should reset after detonation";

        // test 3: decay
        ChargeStackTracker t3 = new ChargeStackTracker();
        for (int i = 0; i < 6; i++) t3.addStack("a:b");
        t3.advance(4f); // 4s * 0.5/s = 2 decayed, 4 remain
        assert t3.getStacks("a:b") == 4 : "Expected 4 after decay, got " + t3.getStacks("a:b");

        // test 4: no negative
        ChargeStackTracker t4 = new ChargeStackTracker();
        t4.addStack("a:b");
        t4.advance(100f);
        assert t4.getStacks("a:b") == 0 : "Should not go negative";

        System.out.println("ChargeStackTrackerTest: all passed.");
    }
}
