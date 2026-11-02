package skills_overhaul.pilot.systems;

/**
 * Regression for review 2026-11-02-0835-pilot B5: after a target switch the flux
 * baseline was 0, so the new target's whole current flux counted as shield
 * damage and the first stack was free; and the debuff stayed on every ship
 * that had ever been painted.
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

        // The debuffed ship is released when the target changes or is lost.
        TargetDebuffHolder<String> h = new TargetDebuffHolder<String>();
        assert h.release("a") == null : "nothing painted yet";
        h.hold("a");
        assert h.release("a") == null : "same target keeps its debuff";
        assert "a".equals(h.release("b")) : "old target is released on switch";
        assert h.release("b") == null : "released once";
        h.hold("b");
        assert "b".equals(h.release(null)) : "target released when the player loses the target";
        assert h.release(null) == null;

        System.out.println("ALL PASSED");
    }
}
