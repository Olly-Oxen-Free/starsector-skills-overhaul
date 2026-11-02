package skills_overhaul.pilot.defense;

/**
 * Regression for review 2026-11-02-0835-pilot B3: the clamp wrote a flat damage
 * offset onto DamageAPI's multiplier (base 1), so a big hit got a negative
 * multiplier, and every hit during the block restarted it, so a cell under
 * sustained fire never reached its cooldown.
 */
public class UnbreakableCellStateTest {
    public static void main(String[] args) {
        // Cell 80/100 (floor 50, allowed 30) hit by a 750-damage shot.
        UnbreakableCellState s = new UnbreakableCellState();
        float mult = s.onHit(80f, 100f, 750f);
        assert mult >= 0f && mult <= 1f : "multiplier must stay in [0,1], got " + mult;
        assert Math.abs(mult * 750f - 30f) < 1e-3f : "hit must be cut to the 30 points above the floor, got " + (mult * 750f);

        // A hit that stays above the floor is untouched.
        assert new UnbreakableCellState().onHit(80f, 100f, 20f) == 1f : "no clamp above floor";
        // A cell already at or below the floor gets no protection.
        assert new UnbreakableCellState().onHit(50f, 100f, 20f) == 1f : "no clamp at floor";

        // Sustained fire: hits during the block must not extend it.
        s = new UnbreakableCellState();
        s.onHit(80f, 100f, 40f);  // starts the 8s block
        for (int i = 0; i < 8; i++) {
            s.advance(1f);
            s.onHit(60f, 100f, 40f);  // another clamping hit each second
        }
        s.advance(0.01f);
        assert s.isOnCooldown() : "block must end 8s after it started, then the cooldown runs";
        assert s.onHit(60f, 100f, 40f) == 1f : "no clamp while on cooldown";

        // Cooldown ends after 15s and the cell can trigger again.
        s.advance(15f);
        assert !s.isOnCooldown();
        assert s.onHit(60f, 100f, 40f) < 1f : "cell re-arms after cooldown";

        System.out.println("ALL PASSED");
    }
}
