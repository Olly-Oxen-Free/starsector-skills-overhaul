package skills_overhaul.pilot.maneuvering;

public class AceKillStackerTest {
    public static void main(String[] args) {
        AceKillStacker s = new AceKillStacker(1.0f);

        s.onFighterKill();
        assert Math.abs(s.getManeuverBonus() - 0.04f) < 1e-4f;

        s.onFrigateKill();
        assert s.getStackCount() == 4 : "1 + 3 = 4 stacks";

        for (int i = 0; i < 5; i++) s.onFighterKill();
        assert s.getStackCount() == 5 : "capped at 5";

        s.advance(7f);
        s.onFighterKill();
        s.advance(7f);
        assert s.getStackCount() == 5 : "refreshed, still alive at 7s";
        s.advance(1.1f);
        assert s.getStackCount() == 0 : "expired after 8s from last kill";

        AceKillStacker sc = new AceKillStacker(0.55f);
        for (int i = 0; i < 5; i++) sc.onFighterKill();
        assert Math.abs(sc.getManeuverBonus() - 5 * 0.04f * 0.55f) < 1e-4f;

        System.out.println("ALL PASSED");
    }
}
