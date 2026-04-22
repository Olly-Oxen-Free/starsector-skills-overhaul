package skills_overhaul.ui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.List;

/**
 * Polls the core UI once per frame (while paused on the CHARACTER tab) and,
 * when the vanilla skill panel is showing, detaches it and attaches our
 * CharacterTabPanelPlugin in its place.
 *
 * On leaving the CHARACTER tab (or any failure to reflect), clears state so the
 * next visit re-installs cleanly. Vanilla children are re-added on restore so
 * other mods / vanilla flow are not affected.
 */
public final class CharacterTabReplacer implements EveryFrameScript {

    private static final String LOG_PREFIX = "[SkillsOverhaul/UI] ";

    private boolean installed = false;
    private UIPanelAPI parent;           // panel the vanilla skill panel sits in
    private UIPanelAPI vanillaPanel;     // the panel we detached (for restore)
    private CustomPanelAPI ourPanel;     // the panel we added
    private boolean reflectionWarned = false;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        if (!Global.getSector().isPaused()) { maybeUninstall(); return; }
        if (Global.getSector().getCampaignUI().getCurrentCoreTab() != CoreUITabId.CHARACTER) {
            maybeUninstall();
            return;
        }
        if (installed) return;

        try {
            install();
        } catch (Throwable t) {
            if (!reflectionWarned) {
                Global.getLogger(CharacterTabReplacer.class).warn(LOG_PREFIX
                        + "reflection failed, leaving vanilla char tab intact", t);
                reflectionWarned = true;
            }
        }
    }

    private void install() {
        // Locate core via encounter dialog (docked) or campaign state (undocked).
        Object state;
        try {
            state = com.fs.state.AppDriver.getInstance().getCurrentState();
        } catch (Throwable t) {
            state = null;
        }
        UIPanelAPI core = null;
        Object dialog = ReflectionUtils.invoke("getEncounterDialog", state);
        if (dialog != null) {
            Object c = ReflectionUtils.invoke("getCoreUI", dialog);
            if (c instanceof UIPanelAPI) core = (UIPanelAPI) c;
        }
        if (core == null) {
            Object c = ReflectionUtils.invoke("getCore", state);
            if (c instanceof UIPanelAPI) core = (UIPanelAPI) c;
        }
        if (core == null) return;

        // Walk into the skill panel: core -> corePanels -> innerPanels where a child
        // has method "canReassign" (vanilla skill panel marker).
        List<Object> corePanels = ReflectionUtils.getChildrenCopy(core);
        UIPanelAPI target = null;
        for (Object cp : corePanels) {
            if (!(cp instanceof UIPanelAPI)) continue;
            for (Object inner : ReflectionUtils.getChildrenCopy((UIPanelAPI) cp)) {
                if (ReflectionUtils.hasMethodOfName("canReassign", inner)) {
                    if (inner instanceof UIPanelAPI) { target = (UIPanelAPI) inner; break; }
                }
            }
            if (target != null) break;
        }
        if (target == null) return;

        UIPanelAPI p = ReflectionUtils.getParent(target);
        if (p == null) return;

        // SiC coexistence: if a "SCSkillMenuPanel" is already present under parent, defer.
        for (Object sib : ReflectionUtils.getChildrenCopy(p)) {
            if (sib.getClass().getSimpleName().contains("SCSkillMenu")) {
                Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX
                        + "Second-in-Command detected; deferring.");
                installed = true; // avoid log spam until tab leaves
                return;
            }
        }

        // Detach vanilla, attach ours.
        p.removeComponent(target);
        parent = p;
        vanillaPanel = target;

        float w = target.getPosition().getWidth();
        float h = target.getPosition().getHeight();
        if (w < 100f) w = 1000f;
        if (h < 100f) h = 700f;

        CharacterTabPanelPlugin plugin = new CharacterTabPanelPlugin();
        ourPanel = Global.getSettings().createCustom(w, h, plugin);
        plugin.attach(ourPanel);
        parent.addComponent(ourPanel);
        ourPanel.getPosition().inTL(0f, 0f);
        installed = true;
    }

    private void maybeUninstall() {
        if (!installed) return;
        try {
            if (parent != null && ourPanel != null) parent.removeComponent(ourPanel);
            if (parent != null && vanillaPanel != null) parent.addComponent(vanillaPanel);
        } catch (Throwable ignored) {}
        installed = false;
        parent = null;
        vanillaPanel = null;
        ourPanel = null;
    }
}
