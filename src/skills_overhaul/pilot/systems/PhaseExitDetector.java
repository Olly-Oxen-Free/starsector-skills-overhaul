package skills_overhaul.pilot.systems;

/**
 * Per-frame phase-exit edge. update() once per frame, then any number of
 * exitedThisFrame() reads in that frame see the same answer.
 */
public final class PhaseExitDetector {
    private boolean wasPhased;
    private boolean exited = false;

    public PhaseExitDetector(boolean initiallyPhased) { this.wasPhased = initiallyPhased; }

    public void update(boolean isPhased) {
        exited = wasPhased && !isPhased;
        wasPhased = isPhased;
    }

    public boolean exitedThisFrame() { return exited; }
}
