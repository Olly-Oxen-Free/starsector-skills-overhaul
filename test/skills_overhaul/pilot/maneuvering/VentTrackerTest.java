package skills_overhaul.pilot.maneuvering;

public class VentTrackerTest {
    public static void main(String[] args) {
        VentTracker t = new VentTracker();

        t.advance(0.1f, false, 500f, 1000f);
        assert Math.abs(t.getVentSprintSpeedBonus() - 0f) < 1e-4f;
        assert Math.abs(t.getVentSprintManeuverBonus() - 0f) < 1e-4f;

        t.advance(0.1f, true, 800f, 1000f);
        float expectedSpeed = 0.80f * 0.15f;
        float expectedManeuver = 0.80f * 0.60f;
        assert Math.abs(t.getVentSprintSpeedBonus() - expectedSpeed) < 1e-4f;
        assert Math.abs(t.getVentSprintManeuverBonus() - expectedManeuver) < 1e-4f;

        assert t.isDeepVentActive();
        t.advance(2f, true, 400f, 1000f);
        assert !t.isDeepVentActive() : "deep vent window expired";

        t.advance(0.1f, false, 0f, 1000f);
        assert Math.abs(t.getVentSprintSpeedBonus() - 0f) < 1e-4f;

        System.out.println("ALL PASSED");
    }
}
