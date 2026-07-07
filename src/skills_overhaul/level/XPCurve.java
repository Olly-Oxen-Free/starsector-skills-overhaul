package skills_overhaul.level;

public final class XPCurve {
    public static final int LEVEL_CAP = 25;
    public static final double BASE = 500.0;
    public static final double EXP  = 1.35;

    public static long xpForLevel(int level) {
        if (level < 1) return 0;
        return Math.round(BASE * Math.pow(level, EXP));
    }

    /** Precomputed cumulative thresholds — levelFromXP runs 3×/sec in LevelReconciler. */
    private static final long[] CUMULATIVE = new long[LEVEL_CAP + 1];
    static {
        long s = 0;
        for (int i = 1; i <= LEVEL_CAP; i++) {
            s += xpForLevel(i);
            CUMULATIVE[i] = s;
        }
    }

    public static long cumulativeXP(int level) {
        if (level <= 0) return 0;
        if (level >= LEVEL_CAP) return CUMULATIVE[LEVEL_CAP];
        return CUMULATIVE[level];
    }

    public static int levelFromXP(long xp) {
        for (int i = 1; i <= LEVEL_CAP; i++) {
            if (xp < CUMULATIVE[i]) return i - 1;
        }
        return LEVEL_CAP;
    }
}
