package skills_overhaul.artificer;

public class BeamSustainedContactTrackerTest {
    public static void main(String[] args) throws Exception {
        BeamSustainedContactTracker t = new BeamSustainedContactTracker();
        Object attacker = "attacker_id";
        Object target = "target_id";

        // Below 3s — no debuff
        t.onBeamHit(attacker, target, 1f);
        t.onBeamHit(attacker, target, 1f);
        assert !t.hasDebuff(target) : "2s contact, no debuff yet";

        t.onBeamHit(attacker, target, 1.5f);
        assert t.hasDebuff(target) : "3.5s contact, debuff active";

        // Reset on miss
        t.onBeamMissed(attacker, target);
        assert !t.hasDebuff(target);

        // Multi-attacker: any attacker with 3s+ activates
        Object attacker2 = "attacker_id_2";
        t.onBeamHit(attacker, target, 4f);
        t.onBeamHit(attacker2, target, 1f);
        assert t.hasDebuff(target) : "attacker has 4s, attacker2 has 1s — debuff still active";

        System.out.println("ALL PASSED");
    }
}
