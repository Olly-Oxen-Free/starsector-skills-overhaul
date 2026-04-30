package skills_overhaul.pilot.gunnery;

public class ThreatAcquisitionStateTest {
    public static void main(String[] args) {
        ThreatAcquisitionState s = new ThreatAcquisitionState();

        s.onHittingTarget(1f);
        assert s.getStacks() == 1;
        s.onHittingTarget(1f);
        assert s.getStacks() == 2;

        for (int i = 0; i < 25; i++) s.onHittingTarget(1f);
        assert s.getStacks() == 20;

        s.advanceNoHit(10f);
        assert s.getStacks() == 0;

        s.onHittingTarget(1f);
        s.onHittingTarget(1f);
        s.onKill();
        assert s.getStacks() == 2 : "stacks preserved on kill";
        s.advanceNoHit(9f);
        assert s.getStacks() == 2 : "carry window still active";
        s.advanceNoHit(1f);
        assert s.getStacks() == 0 : "carry window expired";

        System.out.println("ALL PASSED");
    }
}
