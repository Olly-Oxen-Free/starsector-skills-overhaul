package skills_overhaul.pilot.systems;

/**
 * Regression for review 2026-11-02-0835-pilot B7: the game hands a destroyed
 * ship to the neutral owner (100), so filtering on owner == 1 skipped every
 * kill and Kill Sync / Red Line never triggered.
 */
public class EnemyKillScannerTest {
    public static void main(String[] args) {
        EnemyKillScanner k = new EnemyKillScanner();
        k.seed("enemy", 1, true, false);
        k.seed("ally", 0, true, false);
        k.seed("wing", 1, true, true);

        // Frame 1: everyone alive, no kills.
        assert !k.observe("enemy", 1, true, false);
        assert !k.observe("ally", 0, true, false);
        k.endFrame();

        // Frame 2: the enemy dies and is now a hulk owned by 100.
        assert k.observe("enemy", 100, false, false) : "an enemy that became a neutral hulk is a kill";
        assert !k.observe("ally", 100, false, false) : "a dead ally is not a kill";
        assert !k.observe("wing", 100, false, true) : "fighters are not kills";
        k.endFrame();

        // Frame 3: the hulk is not counted again.
        assert !k.observe("enemy", 100, false, false) : "a kill is reported once";

        System.out.println("ALL PASSED");
    }
}
