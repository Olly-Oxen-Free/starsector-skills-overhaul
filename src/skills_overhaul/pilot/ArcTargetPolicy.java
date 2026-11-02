package skills_overhaul.pilot;

/** Target rule for discharge arcs fired from the player's flagship. */
public final class ArcTargetPolicy {
    private ArcTargetPolicy() {}

    /** The arc's target, or null to skip the arc; never the source itself. */
    public static <T> T dischargeTarget(T source, T nearestEnemy) {
        if (nearestEnemy == null || nearestEnemy == source) return null;
        return nearestEnemy;
    }
}
