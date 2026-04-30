package skills_overhaul.pilot.gunnery;

public class BountyHunterStateTest {
    public static void main(String[] args) {
        BountyHunterState s = new BountyHunterState();

        assert !s.isFluxBonusActive();
        assert s.getAmmoRestoredFraction() == 0f;

        s.onKill();
        assert s.isFluxBonusActive();

        s.advance(7f);
        assert s.isFluxBonusActive();
        s.advance(1f);
        assert !s.isFluxBonusActive();

        s.onKill();
        assert s.isFluxBonusActive();
        s.advance(4f);
        s.onKill();
        s.advance(7f);
        assert s.isFluxBonusActive();

        s.reset();
        s.onKill();
        assert Math.abs(s.getAmmoRestoredFraction() - 0.25f) < 1e-4f;
        s.onKill();
        assert Math.abs(s.getAmmoRestoredFraction() - 0.50f) < 1e-4f;
        s.onKill(); s.onKill();
        assert Math.abs(s.getAmmoRestoredFraction() - 1.00f) < 1e-4f;
        s.onKill();
        assert Math.abs(s.getAmmoRestoredFraction() - 1.00f) < 1e-4f;

        System.out.println("ALL PASSED");
    }
}
