package skills_overhaul.collective;

import java.util.HashSet;
import java.util.Set;

public final class FighterInheritanceTracker {

    private final Set<String> processedWingIds = new HashSet<String>();

    /** Returns true if wingId was not previously tracked (i.e., it is new). */
    public boolean markIfNew(String wingId) {
        return processedWingIds.add(wingId);
    }

    public void reset() {
        processedWingIds.clear();
    }
}
