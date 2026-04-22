package skills_overhaul.level;

public final class XPCurve {
    public static final int LEVEL_CAP = 25;
    public static final double BASE = 500.0;
    public static final double EXP  = 1.35;

    public static long xpForLevel(int level) {
        if (level < 1) return 0;
        return Math.round(BASE * Math.pow(level, EXP));
    }

    public static long cumulativeXP(int level) {
        long s = 0;
        for (int i = 1; i <= level; i++) s += xpForLevel(i);
        return s;
    }

    public static int levelFromXP(long xp) {
        for (int i = 1; i <= LEVEL_CAP; i++) {
            if (xp < cumulativeXP(i)) return i - 1;
        }
        return LEVEL_CAP;
    }
}
