package skills_overhaul.console;

import com.fs.starfarer.api.Global;
import skills_overhaul.suppress.VanillaSuppressor;

/**
 * Player-facing uninstall path. Without this, removing the mod loses all
 * vanilla skills forever: VanillaSuppressor zeroes them on every load and the
 * stash keys are unreadable once the mod is gone.
 *
 * Via Console Commands:
 *   runcode skills_overhaul.console.UninstallHelper.prepareUninstall();
 *   runcode skills_overhaul.console.UninstallHelper.reenableSuppression();
 */
public final class UninstallHelper {

    /**
     * Restore stashed vanilla skill levels and stop suppressing them on load.
     * Save the game afterwards, then remove the mod from the launcher.
     */
    public static void prepareUninstall() {
        Global.getSector().getMemoryWithoutUpdate()
                .set(VanillaSuppressor.K_SUPPRESSION_DISABLED, true);
        VanillaSuppressor.restoreAll();
        System.out.println("Vanilla skills restored; suppression disabled.");
        System.out.println("SAVE THE GAME NOW, then disable Skills Overhaul in the launcher.");
        System.out.println("(To undo: runcode skills_overhaul.console.UninstallHelper.reenableSuppression();)");
    }

    /** Undo prepareUninstall(): re-suppress vanilla skills and keep playing. */
    public static void reenableSuppression() {
        Global.getSector().getMemoryWithoutUpdate()
                .unset(VanillaSuppressor.K_SUPPRESSION_DISABLED);
        VanillaSuppressor.applyOnEveryLoad();
        System.out.println("Vanilla skill suppression re-enabled.");
    }

    private UninstallHelper() {}
}
