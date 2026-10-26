package skills_overhaul.ui.host;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import skills_overhaul.SkillsOverhaulModPlugin;
import skills_overhaul.config.ConfigLoader;

/**
 * Routes {@link CharTabHost} outcomes. Under Second-in-Command, or when the vanilla panel
 * cannot be probed, the tab is not replaced and the {@link SkillsHotkeyListener} /
 * {@link DialogHost} path is the way in. This class writes the single INFO / WARN line for
 * each case and tells the player about the hotkey once per game session.
 *
 * State is static and reset by {@link #reset()} from onGameLoad, so a reload in the same JVM
 * re-arms the message and log lines.
 */
public final class HostRouter implements HostEvents {

    private static final Logger LOG = Global.getLogger(HostRouter.class);

    private static boolean messageShown;
    private static boolean sicLogged;
    private static boolean probeLogged;

    /** Clears the once-per-session flags. Call from onGameLoad before registering hosts. */
    public static void reset() {
        messageShown = false;
        sicLogged = false;
        probeLogged = false;
    }

    @Override
    public void onSicDetected() {
        if (!sicLogged) {
            sicLogged = true;
            LOG.info(SkillsOverhaulModPlugin.LOG_PREFIX
                    + "Second in Command detected; not replacing the character tab skill panel."
                    + " Skills dialog is on the hotkey.");
        }
        notifyHotkeyOnce();
    }

    @Override
    public void onProbeExhausted(String lastReason) {
        if (!probeLogged) {
            probeLogged = true;
            LOG.warn(SkillsOverhaulModPlugin.LOG_PREFIX
                    + "could not replace the character tab skill panel (" + lastReason
                    + "); skills dialog is on the hotkey.");
        }
        notifyHotkeyOnce();
    }

    private void notifyHotkeyOnce() {
        if (!claimMessage()) return;
        try {
            String key = Keyboard.getKeyName(ConfigLoader.get().uiHotkey);
            Global.getSector().getCampaignUI().addMessage(messageText(key));
        } catch (Throwable t) {
            LOG.warn(SkillsOverhaulModPlugin.LOG_PREFIX + "could not show hotkey message", t);
        }
    }

    /** True the first time after {@link #reset()}; false afterwards. */
    static boolean claimMessage() {
        if (messageShown) return false;
        messageShown = true;
        return true;
    }

    static String messageText(String keyName) {
        return "Skills Overhaul: press " + keyName + " to open the skills screen.";
    }
}
