package skills_overhaul.pilot.systems;

public class PhaseExitDetectorTest {
    public static void main(String[] args) {
        PhaseExitDetector d = new PhaseExitDetector(false);

        d.update(true);
        assert !d.exitedThisFrame() : "entering phase is not an exit";

        d.update(false);
        // Void Strike / Reactive Systems read the edge first, Phase Mastery reads it
        // later in the same frame; both must see it.
        assert d.exitedThisFrame() : "first reader sees the exit";
        assert d.exitedThisFrame() : "later reader in the same frame still sees the exit";

        d.update(false);
        assert !d.exitedThisFrame() : "exit is reported for one frame only";

        PhaseExitDetector started = new PhaseExitDetector(true);
        started.update(false);
        assert started.exitedThisFrame() : "starting phased then dropping is an exit";

        System.out.println("ALL PASSED");
    }
}
