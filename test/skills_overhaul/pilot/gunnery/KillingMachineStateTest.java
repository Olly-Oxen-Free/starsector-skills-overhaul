package skills_overhaul.pilot.gunnery;

public class KillingMachineStateTest {
    public static void main(String[] args) {
        KillingMachineState s = new KillingMachineState();

        assert s.getStacks() == 0;
        assert Math.abs(s.getDamageBonus() - 0f) < 1e-4f;

        s.onKill();
        assert s.getStacks() == 1;
        assert Math.abs(s.getDamageBonus() - 0.03f) < 1e-4f;

        for (int i = 0; i < 10; i++) s.onKill();
        assert s.getStacks() == 5 : "capped at 5";
        assert Math.abs(s.getDamageBonus() - 0.15f) < 1e-4f;

        assert Math.abs(s.getGlassCannonPenaltyRelief() - 0.10f) < 1e-4f;

        s.reset();
        assert s.getStacks() == 0;

        System.out.println("ALL PASSED");
    }
}
