package skills_overhaul.pilot.gunnery;

public class LockOnStateTest {
    public static void main(String[] args) {
        LockOnState s = new LockOnState();

        assert !s.isLocked();

        s.advance(2f, "target_a");
        assert !s.isLocked();

        s.advance(1f, "target_a");
        assert s.isLocked();

        s.advance(0.1f, "target_b");
        assert !s.isLocked();

        s.advance(3f, "target_c");
        assert s.isLocked();
        s.advance(0.1f, null);
        assert !s.isLocked();

        System.out.println("ALL PASSED");
    }
}
