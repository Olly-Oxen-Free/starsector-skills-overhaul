package skills_overhaul.pilot.defense;

public class AdamantineStateTest {
    public static void main(String[] args) {
        AdamantineState s = new AdamantineState(1000f);

        s.updateFlux(600f);
        assert Math.abs(s.getBuffer() - 200f) < 1e-4f : "buffer capped at 20% of 1000";

        s.updateFlux(100f);
        assert Math.abs(s.getBuffer() - 100f) < 1e-4f : "buffer = flux when below cap";

        boolean discharged = s.absorbHullDamage(80f);
        assert !discharged;
        assert Math.abs(s.getBuffer() - 20f) < 1e-4f;

        discharged = s.absorbHullDamage(30f);
        assert discharged : "buffer exhausted → discharge";
        assert Math.abs(s.getBuffer() - 0f) < 1e-4f;

        assert !s.canDischarge();
        s.advance(4f);
        assert !s.canDischarge();
        s.advance(1f);
        assert s.canDischarge();

        System.out.println("ALL PASSED");
    }
}
