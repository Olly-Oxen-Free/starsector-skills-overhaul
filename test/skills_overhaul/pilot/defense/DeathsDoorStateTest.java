package skills_overhaul.pilot.defense;

public class DeathsDoorStateTest {
    public static void main(String[] args) {
        DeathsDoorState s = new DeathsDoorState();

        assert !s.isImmune();
        assert !s.isUsed();

        boolean triggered = s.tryTrigger(0.09f);
        assert triggered;
        assert s.isUsed();
        assert s.isImmune();

        s.advance(3f);
        assert s.isImmune();

        s.advance(1f);
        assert !s.isImmune();

        boolean triggered2 = s.tryTrigger(0.05f);
        assert !triggered2;
        assert !s.isImmune();

        s.reset();
        assert !s.isUsed();
        assert s.tryTrigger(0.05f);

        System.out.println("ALL PASSED");
    }
}
