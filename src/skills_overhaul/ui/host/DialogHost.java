package skills_overhaul.ui.host;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import skills_overhaul.ui.view.SkillTreeView;

import java.util.List;
import java.util.Map;

/**
 * Fallback host: shows {@link SkillTreeView} in a custom visual dialog opened from the
 * campaign map (hotkey path for players under Second-in-Command, or when the character-tab
 * mount fails).
 *
 * <p>Opening shows a dummy {@link InteractionDialogPlugin} whose {@code init} hides the text and
 * visual panels and calls {@code showCustomVisualDialog}. Nothing closes that dialog for us:
 * Esc with no pending purchases, or the close button, runs the LunaLib close sequence
 * ({@code showTextPanel; showVisualPanel; dismissDialog; dismiss}). Esc with pending purchases
 * belongs to the view, which consumes it and clears the pending set.
 */
public final class DialogHost {

    /** Dialog size as a fraction of the screen. */
    private static final float SCREEN_FRACTION = 0.9f;
    private static final float CLOSE_W = 90f;
    private static final float CLOSE_H = 22f;
    private static final float CLOSE_PAD = 16f;
    private static final String BTN_CLOSE = "__so_dialog_close__";

    private DialogHost() {}

    /**
     * Opens the skills dialog from the campaign map. Returns false (and opens nothing) when a
     * dialog is already showing or the game refuses to show one.
     */
    public static boolean open() {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null || ui.getCurrentInteractionDialog() != null) return false;
        try {
            return ui.showInteractionDialog(new DummyDialogPlugin(), Global.getSector().getPlayerFleet());
        } catch (Throwable t) {
            Global.getLogger(DialogHost.class).error("[SkillsOverhaul/UI] dialog host failed to open", t);
            return false;
        }
    }

    /** Empty interaction plugin whose only job is to show the custom visual dialog. */
    private static final class DummyDialogPlugin implements InteractionDialogPlugin {
        @Override
        public void init(InteractionDialogAPI dialog) {
            dialog.hideVisualPanel();
            dialog.hideTextPanel();
            dialog.setPromptText("");
            float w = Global.getSettings().getScreenWidth() * SCREEN_FRACTION;
            float h = Global.getSettings().getScreenHeight() * SCREEN_FRACTION;
            // The view (and its panel plugin) exists before showCustomVisualDialog is called.
            dialog.showCustomVisualDialog(w, h, new Delegate(dialog, w, h));
        }

        @Override public void optionSelected(String optionText, Object optionData) {}
        @Override public void optionMousedOver(String optionText, Object optionData) {}
        @Override public void advance(float amount) {}
        @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
        @Override public Object getContext() { return null; }
        @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
    }

    /**
     * Hosts the view. Its panel plugin is a frame wrapped around the view: the frame owns the
     * dialog's panel (close button, Esc-with-no-pending) and the view's own panel is mounted
     * inside it, so the view keeps its input and rendering unchanged.
     */
    private static final class Delegate implements CustomVisualDialogDelegate {
        private final InteractionDialogAPI dialog;
        private final SkillTreeView view;
        private final Frame frame;
        private DialogCallbacks callbacks;
        private boolean closed;

        Delegate(InteractionDialogAPI dialog, float w, float h) {
            this.dialog = dialog;
            this.view = new SkillTreeView(w, h);
            this.frame = new Frame(this);
        }

        @Override
        public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
            this.callbacks = callbacks;
            frame.attach(panel, view);
        }

        @Override public CustomUIPanelPlugin getCustomPanelPlugin() { return frame; }
        @Override public float getNoiseAlpha() { return 0f; }
        @Override public void advance(float amount) {}

        /** The argument's meaning is undocumented; any call means the dialog is gone. */
        @Override
        public void reportDismissed(int option) {
            if (closed) return;
            closed = true;
            view.discard();
        }

        boolean hasPending() { return view.hasPending(); }

        void close() {
            if (closed) return;
            closed = true;
            view.discard();
            dialog.showTextPanel();
            dialog.showVisualPanel();
            if (callbacks != null) callbacks.dismissDialog();
            dialog.dismiss();
        }
    }

    /** Plugin of the dialog's panel: mounts the view, adds a close button, handles Esc. */
    private static final class Frame extends BaseCustomUIPanelPlugin {
        private final Delegate owner;

        Frame(Delegate owner) { this.owner = owner; }

        void attach(CustomPanelAPI panel, SkillTreeView view) {
            PositionAPI vp = panel.addComponent(view.getPanel());
            if (vp != null) vp.inTL(0f, 0f);

            // Added after the view so it sits on top; the view's tab strip is centred, so
            // the top-right corner is free.
            TooltipMakerAPI el = panel.createUIElement(CLOSE_W, CLOSE_H, false);
            el.addButton("Close", BTN_CLOSE, Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.C2_MENU, CLOSE_W, CLOSE_H, 0f);
            PositionAPI cp = panel.addUIElement(el);
            if (cp != null) cp.inTR(CLOSE_PAD, 0f);
        }

        @Override
        public void buttonPressed(Object id) {
            if (BTN_CLOSE.equals(id)) owner.close();
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            if (events == null) return;
            for (InputEventAPI e : events) {
                if (e.isConsumed()) continue;
                if (!e.isKeyDownEvent() || e.getEventValue() != Keyboard.KEY_ESCAPE) continue;
                // With pending purchases the view owns Esc (it clears them); never consume
                // it twice, whichever plugin sees the event first.
                if (owner.hasPending()) continue;
                e.consume();
                owner.close();
                return;
            }
        }
    }
}
