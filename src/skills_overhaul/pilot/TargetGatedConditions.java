package skills_overhaul.pilot;

/**
 * Conditions for pilot buffs that depend on the flagship's current target.
 * Each takes whether a target exists, so the lanes can evaluate them every
 * frame and remove the buff when the target is cleared.
 */
public final class TargetGatedConditions {
    private TargetGatedConditions() {}

    /** Knife Fighter: target within 300u. */
    public static boolean knifeFighter(boolean hasTarget, float dist) {
        return hasTarget && dist <= 300f;
    }

    /** Flanking Mastery: attacking from more than 45° off the target's facing. */
    public static boolean flanking(boolean hasTarget, float angleDiff) {
        return hasTarget && angleDiff > 45f;
    }

    /** Dragon Slayer: +10% per hull size the target is larger, capped at 30%. */
    public static float dragonSlayerBonus(boolean hasTarget, int sizeDiff) {
        if (!hasTarget) return 0f;
        return Math.min(0.30f, Math.max(0f, sizeDiff * 0.10f));
    }

    /** Point Blank: target within 400u. */
    public static boolean pointBlank(boolean hasTarget, float dist) {
        return hasTarget && dist <= 400f;
    }

    /** Dead Eye: flux discount beyond 75% of an approximate 1000u range. */
    public static boolean deadEyeFluxDiscount(boolean hasTarget, float dist) {
        return hasTarget && dist > 750f;
    }
}
