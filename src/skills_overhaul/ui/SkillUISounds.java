package skills_overhaul.ui;

import com.fs.starfarer.api.Global;
import skills_overhaul.state.Tree;

/**
 * Vanilla character-screen SFX from {@code data/config/sounds.json} (e.g. {@code combat1}…{@code combat5}).
 */
public final class SkillUISounds {

    private SkillUISounds() {}

    /** Story / respec-style reset (same id vanilla uses for aptitude reset). */
    public static void playRespecReset() {
        try {
            Global.getSoundPlayer().playUISound("ui_char_reset", 1f, 0.75f);
        } catch (Throwable ignored) {}
    }

    /**
     * Tier-indexed aptitude pick sounds: {@code combat1..5}, {@code leadership1..5}, {@code industry1..5}.
     * {@code tier} is the node's row index (0 = root); clamped to 1..5 for the sound key.
     */
    public static void playTierSkillSound(Tree tree, int tier, float volume) {
        int idx = Math.min(5, Math.max(1, tier + 1));
        String id;
        switch (tree) {
            case PILOT:
                id = "combat" + idx;
                break;
            case COMMANDER:
                id = "leadership" + idx;
                break;
            case ADMIN:
                id = "industry" + idx;
                break;
            default:
                id = "ui_char_level_up";
                break;
        }
        try {
            Global.getSoundPlayer().playUISound(id, 1f, volume);
        } catch (Throwable ignored) {}
    }
}
