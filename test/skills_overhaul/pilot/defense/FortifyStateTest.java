package skills_overhaul.pilot.defense;

public class FortifyStateTest {
    public static void main(String[] args) {
        FortifyState s = new FortifyState();

        assert s.getStacks() == 0;
        assert Math.abs(s.getDRMult() - 1.0f) < 1e-4f;

        s.onQualifyingHit();
        assert s.getStacks() == 1;
        assert Math.abs(s.getDRMult() - 0.95f) < 1e-4f;

        for (int i = 0; i < 10; i++) s.onQualifyingHit();
        assert s.getStacks() == 5;
        assert Math.abs(s.getDRMult() - 0.75f) < 1e-4f;

        s.advance(0.6f);
        assert s.getStacks() == 5 : "no decay in first 0.5s grace";

        s.advance(1.0f);
        assert s.getStacks() == 4 : "1 stack decayed after 1s no-hit";

        s.onQualifyingHit();
        assert s.getStacks() == 5;
        s.advance(0.3f);
        assert s.getStacks() == 5;

        s.reset();
        assert s.getStacks() == 0;

        System.out.println("ALL PASSED");
    }
}
