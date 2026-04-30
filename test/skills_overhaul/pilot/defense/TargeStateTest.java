package skills_overhaul.pilot.defense;

public class TargeStateTest {
    public static void main(String[] args) {
        TargeState s = new TargeState();

        assert Math.abs(s.getWeaponDamageMult() - 1.0f) < 1e-4f;

        s.recordHullLost(0.10f);
        assert Math.abs(s.getWeaponDamageMult() - 1.10f) < 1e-4f;

        s.recordHullLost(0.40f);
        assert Math.abs(s.getWeaponDamageMult() - 1.50f) < 1e-4f;

        s.recordHullLost(0.30f);
        assert Math.abs(s.getWeaponDamageMult() - 1.50f) < 1e-4f;

        assert Math.abs(s.getRegenPerSecondFraction() - 0.001f) < 1e-6f;

        s.reset();
        assert Math.abs(s.getWeaponDamageMult() - 1.0f) < 1e-4f;

        System.out.println("ALL PASSED");
    }
}
