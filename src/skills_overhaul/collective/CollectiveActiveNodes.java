package skills_overhaul.collective;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared mutable set of active Collective node IDs.
 * Set by SOCollectiveT0Handler and read by CollectiveCombatPlugin.
 */
public final class CollectiveActiveNodes {
    public static final Set<String> ACTIVE = new HashSet<>();

    public static boolean isActive(String nodeId) {
        return ACTIVE.contains(nodeId);
    }

    private CollectiveActiveNodes() {}
}
