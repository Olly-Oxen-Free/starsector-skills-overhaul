package skills_overhaul.xp;

/** reviews/2026-11-03-1635-xp.md B1 (flagship latch). */
public class PilotBattleStateTest {
    public static void main(String[] args) {
        // B1: no player ship at init; flagship deploys on a later frame.
        PilotBattleState s = new PilotBattleState();
        assert !s.observePlayerShip(null, false, false) : "nothing to latch before deployment";
        assert !s.isLatched();
        assert s.observePlayerShip("flag", true, false) : "flagship must latch once deployed";
        assert !s.firstShipEverDied();
        // Flagship destroyed, command transferred to another ship.
        s.observePlayerShip("other", true, false);
        assert s.firstShipEverDied() : "transfer after flagship loss must void the survival rule";

        // A shuttle pod is never latched as the flagship.
        PilotBattleState p = new PilotBattleState();
        assert !p.observePlayerShip("pod", true, true);
        assert !p.isLatched();

        System.out.println("ALL PASSED");
    }
}
