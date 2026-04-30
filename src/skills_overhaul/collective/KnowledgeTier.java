package skills_overhaul.collective;

public enum KnowledgeTier {
    NASCENT(0, 0),      // 0–49 Knowledge
    AWARE(1, 50),       // 50–149
    INTEGRATED(2, 150), // 150–299
    ASCENDANT(3, 300),  // 300–499
    TRANSCENDENT(4, 500); // 500+

    // [TUNE] thresholds — adjust based on campaign pacing playtest

    public final int ordinalTier;
    public final int threshold;

    KnowledgeTier(int ordinalTier, int threshold) {
        this.ordinalTier = ordinalTier;
        this.threshold = threshold;
    }

    public static KnowledgeTier fromKnowledge(float knowledge) {
        KnowledgeTier result = NASCENT;
        for (KnowledgeTier t : values()) {
            if (knowledge >= t.threshold) result = t;
        }
        return result;
    }

    /** Aura bonus for Command Hierarchy: +2% weapon damage and +2% flux dissipation per tier. */
    public float getAuraBonusPct() {
        return ordinalTier * 2f;
    }
}
