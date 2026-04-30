package skills_overhaul.collective;

import java.util.HashSet;
import java.util.Set;

public final class SOCriticalStateTracker {

    private final Set<String> triggered = new HashSet<String>();

    public void markTriggered(String shipId) {
        triggered.add(shipId);
    }

    public boolean hasTriggered(String shipId) {
        return triggered.contains(shipId);
    }

    public void reset() {
        triggered.clear();
    }
}
