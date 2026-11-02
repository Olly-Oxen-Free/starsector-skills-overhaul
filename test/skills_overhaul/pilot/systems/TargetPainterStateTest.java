package skills_overhaul.pilot.systems;

/**
 * Regression for review 2026-11-02-0835-pilot B5: after a target switch the flux
 * baseline was 0, so the new target's whole current flux counted as shield
 * damage and the first stack was free. (Removing the debuff from earlier
 * targets was already fixed on audit/2026-10-02 in SystemsCombatPlugin.)
 */
public class TargetPainterStateTest {
    public static void main(String[] args) {
        // Switch to a target already sitting at 400 flux: no free stack.
        TargetPainterState p = new TargetPainterState();
        p.onTargetSwitch(400f);
        p.onTargetFlux(400f, 0f);
        assert p.getStacks() == 0 : "existing flux on the new target is not damage";

        // Real shield damage still builds stacks (150 threshold, 1/s limit).
        p.onTargetFlux(560f, 0.1f);
        assert p.getStacks() == 1 : "160 flux of damage gives a stack";
        p.onTargetFlux(720f, 1.2f);
        assert p.getStacks() == 2;
        assert Math.abs(p.getShieldEfficiencyDebuff() - 0.06f) < 1e-6f;

        // Switching resets stacks.
        p.onTargetSwitch(0f);
        assert p.getStacks() == 0;

        System.out.println("ALL PASSED");
    }
}
