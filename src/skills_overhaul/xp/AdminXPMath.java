package skills_overhaul.xp;

public final class AdminXPMath {
    private AdminXPMath() {}

    public static float foundingXP(int n, float base) {
        return base / (1f + n);
    }

    public static float conquestXP(int nextFoundingN, int marketSize, float militaryBonus, float foundingBase) {
        float mil = militaryBonus * (marketSize / 10f);
        return mil + foundingXP(nextFoundingN, foundingBase);
    }

    public static float incomeXP(float monthlyCreditsNet, float rate) {
        if (monthlyCreditsNet <= 0f) return 0f;
        return (float) (Math.log10(1 + monthlyCreditsNet) * rate);
    }

    public static float streakXP(int streakMonths, int capMonths, float base) {
        int eff = Math.max(0, Math.min(capMonths, streakMonths));
        return eff * base;
    }

    public static int updateStreak(int prev, int stability, int threshold) {
        if (stability >= threshold) return prev + 1;
        if (stability >= threshold - 3) return prev;
        return 0;
    }

    public static float modifier(boolean selfAdmin, boolean vassal) {
        return modifier(selfAdmin, vassal, 0.25f, 0.50f);
    }
    public static float modifier(boolean selfAdmin, boolean vassal, float vassalMult, float adminMult) {
        if (selfAdmin) return 1.00f;
        if (vassal)    return vassalMult;
        return adminMult;
    }
}
