package skills_overhaul.corsair;

public enum InfamyTier {
    KNOWN(5),
    WANTED(10),
    NOTORIOUS(15),
    FEARED(20),
    LEGEND(30),
    APEX(40),
    DREAD(50);

    public final int threshold;

    InfamyTier(int threshold) { this.threshold = threshold; }

    /** Highest tier whose threshold ≤ infamy, or null if below KNOWN. */
    public static InfamyTier fromInfamy(int infamy) {
        InfamyTier best = null;
        for (InfamyTier t : values()) {
            if (infamy >= t.threshold) best = t;
            else break;
        }
        return best;
    }
}
