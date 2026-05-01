package skills_overhaul.pilot.maneuvering;

import java.util.ArrayList;
import java.util.List;

public final class PhaseStateTracker {
    public interface PhaseExitListener { void onPhaseExit(float timeInPhase); }

    private boolean wasPhased = false;
    private float timeInPhase = 0f;
    private final List<PhaseExitListener> exitListeners = new ArrayList<>();

    public void addExitListener(PhaseExitListener l) { exitListeners.add(l); }

    public void advance(float dt, boolean isPhased) {
        if (isPhased) {
            timeInPhase += dt;
        } else if (wasPhased) {
            float duration = timeInPhase;
            timeInPhase = 0f;
            for (PhaseExitListener l : exitListeners) l.onPhaseExit(duration);
        }
        wasPhased = isPhased;
    }

    public boolean isPhased() { return wasPhased; }
    public float getTimeInPhase() { return timeInPhase; }
    public void reset() { wasPhased = false; timeInPhase = 0f; }
}
