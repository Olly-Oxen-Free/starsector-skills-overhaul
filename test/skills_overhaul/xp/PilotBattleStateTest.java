package skills_overhaul.xp;

/** reviews/2026-11-03-1635-xp.md B1 (flagship latch) and B2 (battle-end detection). */
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

        // B2: gap between reinforcement waves is not the end of the battle.
        assert !PilotBattleState.battleEnded(false, true, 3, false) : "reserves still to deploy";
        // First frame, before any enemy has spawned.
        assert !PilotBattleState.battleEnded(false, false, 0, false) : "no enemy seen yet";
        assert !PilotBattleState.battleEnded(false, true, 0, true);
        assert PilotBattleState.battleEnded(false, true, 0, false) : "real victory";
        assert PilotBattleState.battleEnded(true, false, 5, true) : "engine combat-over always ends";

        System.out.println("ALL PASSED");
    }
}
