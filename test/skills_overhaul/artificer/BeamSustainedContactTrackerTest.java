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

        // getTargets: reflects per-attacker tracked targets, used to reconcile lost contact
        BeamSustainedContactTracker g = new BeamSustainedContactTracker();
        assert g.getTargets("nobody").isEmpty() : "unknown attacker has no targets";
        g.onBeamHit("a", "x", 1f);
        g.onBeamHit("a", "y", 1f);
        assert g.getTargets("a").size() == 2 : "two targets tracked";
        assert g.getTargets("a").contains("x") && g.getTargets("a").contains("y");
        g.onBeamMissed("a", "x");
        assert g.getTargets("a").size() == 1 && g.getTargets("a").contains("y") : "x dropped on miss";
        // returned set is a snapshot — mutating it must not corrupt internal state
        g.getTargets("a").clear();
        assert g.getTargets("a").size() == 1 : "getTargets returns a defensive copy";

        System.out.println("ALL PASSED");
    }
}
