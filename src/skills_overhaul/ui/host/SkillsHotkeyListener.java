package skills_overhaul.ui.host;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;

import skills_overhaul.config.ConfigLoader;

import java.util.List;

/**
 * Opens {@link DialogHost} on the configured hotkey ({@code ui_hotkey}, default K) from the
 * campaign map and consumes the key event it acts on. Registration belongs to the mod plugin.
 */
public final class SkillsHotkeyListener implements CampaignInputListener {

    /**
     * Run before other listeners so we see the key before anything else acts on it. Console
     * Commands uses {@code Integer.MAX_VALUE - 10}; stay just below it so its console key
     * still wins.
     */
    private static final int PRIORITY = Integer.MAX_VALUE - 20;

    @Override
    public int getListenerInputPriority() { return PRIORITY; }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
        if (events == null || events.isEmpty()) return;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null) return;
        int configured = ConfigLoader.get().uiHotkey;
        boolean hasDialog = ui.getCurrentInteractionDialog() != null;
        boolean showingMenu = ui.isShowingMenu();
        boolean coreTab = ui.getCurrentCoreTab() != null;

        for (InputEventAPI e : events) {
            // Event data throws on a consumed event, so check isConsumed() before reading it.
            if (e.isConsumed()) continue;
            boolean keyDown = e.isKeyDownEvent();
            int key = keyDown ? e.getEventValue() : -1;
            if (!shouldOpen(false, keyDown, key, configured, hasDialog, showingMenu, coreTab)) continue;
            e.consume();
            DialogHost.open();
            return;
        }
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {}

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {}

    /**
     * Pure guard (no game or lwjgl calls): open only on an unconsumed key-down of the
     * configured key while no interaction dialog, menu or core tab is showing. The UI checks
     * also keep the key from firing while the player types in a text field.
     */
    public static boolean shouldOpen(boolean consumed, boolean keyDown, int eventKey, int configuredKey,
                                     boolean hasInteractionDialog, boolean showingMenu, boolean coreTabOpen) {
        if (consumed || !keyDown) return false;
        if (eventKey != configuredKey) return false;
        return !hasInteractionDialog && !showingMenu && !coreTabOpen;
    }
}
