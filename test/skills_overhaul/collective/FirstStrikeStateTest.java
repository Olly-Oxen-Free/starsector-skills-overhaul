package skills_overhaul.collective;

public class FirstStrikeStateTest {
    public static void main(String[] args) {
        // test 1: initial state UNENGAGED
        SOFirstStrikeTracker t = new SOFirstStrikeTracker();
        assert t.getState("s1") == SOFirstStrikeTracker.FSState.UNENGAGED;

        // test 2: transition to STRUCK on contact
        t.notifyContact("s1");
        assert t.getState("s1") == SOFirstStrikeTracker.FSState.STRUCK;
        assert t.getDecayElapsed("s1") == 0f;

        // test 3: decay after 15s → 50% bonus
        t.advance("s1", 15f);
        assert t.getState("s1") == SOFirstStrikeTracker.FSState.DECAYING;
        assert Math.abs(t.getDamageMultiplier("s1") - 0.15f) < 0.001f
                : "Expected 0.15 got " + t.getDamageMultiplier("s1");

        // test 4: fully decayed after 30s total
        t.advance("s1", 15f);
        assert Math.abs(t.getDamageMultiplier("s1")) < 0.001f;

        System.out.println("FirstStrikeStateTest: all passed.");
    }
}
