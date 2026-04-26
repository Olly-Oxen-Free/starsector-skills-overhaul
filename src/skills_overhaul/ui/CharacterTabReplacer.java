package skills_overhaul.ui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.state.AppDriver;

import skills_overhaul.state.Tree;
import skills_overhaul.tree.TreeRegistry;

import java.util.List;

/**
 * Polls the core UI once per frame (while paused on the CHARACTER tab) and,
 * when the vanilla skill panel is showing, hides it and attaches our
 * CharacterTabPanelPlugin in its place.
 *
 * Instead of removeComponent (which the layout manager often reverts),
 * we HIDE the vanilla panel by setting its size to 0 and moving it off-screen.
 * On uninstall, we restore its original size and position.
 */
public final class CharacterTabReplacer implements EveryFrameScript {

    private static final String LOG_PREFIX = "[SkillsOverhaul/UI] ";

    private boolean installed = false;
    private UIPanelAPI vanillaParent;    // parent of the vanilla skill panel (for restore)
    private UIPanelAPI vanillaPanel;
    private UIPanelAPI mountParent;      // where OUR panel was added (core UI)
    private CustomPanelAPI ourPanel;
    private boolean reflectionWarned = false;

    // Original vanilla panel state for restore
    private float vanillaOrigX, vanillaOrigY, vanillaOrigW, vanillaOrigH;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        if (!Global.getSector().isPaused()) { maybeUninstall(); return; }
        CoreUITabId tab = Global.getSector().getCampaignUI().getCurrentCoreTab();
        if (tab != CoreUITabId.CHARACTER) {
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
        Object rawState = AppDriver.getInstance().getCurrentState();
        if (!(rawState instanceof CampaignState)) {
            Global.getLogger(CharacterTabReplacer.class).warn(LOG_PREFIX + "install: rawState is not CampaignState, got " + (rawState != null ? rawState.getClass().getName() : "null"));
            return;
        }
        CampaignState state = (CampaignState) rawState;

        // DEBUG: Log state discovery process
        Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX + "install: starting panel discovery");

        UIPanelAPI core = null;
        Object dialog = ReflectionUtils.invoke("getEncounterDialog", state);
        if (dialog != null) {
            Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX + "install: found encounterDialog=" + dialog.getClass().getName());
            Object c = ReflectionUtils.invoke("getCoreUI", dialog);
            if (c instanceof UIPanelAPI) core = (UIPanelAPI) c;
        }
        if (core == null) {
            Object c = ReflectionUtils.invoke("getCore", state);
            if (c instanceof UIPanelAPI) {
                core = (UIPanelAPI) c;
                Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX + "install: found core from state");
            }
        }
        if (core == null) {
            Global.getLogger(CharacterTabReplacer.class).warn(LOG_PREFIX + "install: could not find core UI panel, bailing");
            return;
        }

        UIPanelAPI target = null;
        Object currentTab = ReflectionUtils.invoke("getCurrentTab", core);
        if (currentTab instanceof UIPanelAPI) {
            target = findLargestPanelWithCanReassign((UIPanelAPI) currentTab);
        }
        if (target == null) {
            target = findLargestPanelWithCanReassign(core);
        }
        if (target == null) return;

        UIPanelAPI p = ReflectionUtils.getParent(target);
        if (p == null) return;

        // SiC coexistence check
        for (Object sib : ReflectionUtils.getChildrenCopy(p)) {
            if (sib.getClass().getSimpleName().contains("SCSkillMenu")) {
                installed = true;
                return;
            }
        }

        // Save original position and size for restore
        com.fs.starfarer.api.ui.PositionAPI tPos = target.getPosition();
        vanillaOrigX = tPos.getX();
        vanillaOrigY = tPos.getY();
        vanillaOrigW = tPos.getWidth();
        vanillaOrigH = tPos.getHeight();

        vanillaParent = p;
        vanillaPanel  = target;

        // Remove the vanilla skill panel from its immediate parent so it
        // doesn't keep drawing behind us. If removal fails, push it off-screen.
        Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX + "install: target class="
                + target.getClass().getName() + " size=" + tPos.getWidth() + "x" + tPos.getHeight());
        try {
            p.removeComponent(target);
            Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX + "install: removeComponent succeeded");
        } catch (Throwable t) {
            Global.getLogger(CharacterTabReplacer.class).warn(LOG_PREFIX + "install: removeComponent FAILED, hiding instead", t);
            try {
                tPos.setSuspendRecompute(true);
                tPos.setSize(0.1f, 0.1f);
                tPos.setLocation(-5000f, -5000f);
                tPos.setSuspendRecompute(false);
            } catch (Throwable ignored) {}
        }

        // Also hide the surrounding vanilla character-tab chrome (portrait card,
        // stats, etc.) so our panel becomes the single unified UI visible on
        // this tab. We track their original size+position for restore.
        hideSiblings(p, target);

        // --- Panel sizing & centered mount ---------------------------------
        // Diagnostics: widest tree's content width.
        float maxGridW = 0f, maxGridH = 0f;
        for (Tree t : Tree.values()) {
            for (skills_overhaul.tree.TreeDef def : TreeRegistry.getTrees(t)) {
                PathTreeBuilder.Metrics m = PathTreeBuilder.computeMetrics(def);
                if (m.totalW > maxGridW) maxGridW = m.totalW;
                if (m.totalH > maxGridH) maxGridH = m.totalH;
            }
        }

        float screenW = Global.getSettings().getScreenWidth();
        float screenH = Global.getSettings().getScreenHeight();
        float panelW = Math.min(1700f, screenW * 0.90f);
        float panelH = Math.min(950f,  screenH * 0.88f);

        // Mount at the core UI panel (a large, typically screen-sized ancestor).
        // Centering via inMid() then places us dead-center on screen.
        mountParent = core;

        CharacterTabPanelPlugin plugin = new CharacterTabPanelPlugin();
        ourPanel = Global.getSettings().createCustom(panelW, panelH, plugin);
        mountParent.addComponent(ourPanel);
        try {
            ourPanel.getPosition().inMid();
        } catch (Throwable ignored) {
            // Fallback: manual center using the core panel's bounds.
            com.fs.starfarer.api.ui.PositionAPI cPos = mountParent.getPosition();
            float cx = (cPos.getWidth()  - panelW) / 2f;
            float cy = (cPos.getHeight() - panelH) / 2f;
            ourPanel.getPosition().inTL(cx, cy);
        }
        plugin.attach(ourPanel);

        Global.getLogger(CharacterTabReplacer.class).info(LOG_PREFIX
                + "install: mounted " + CharacterTabPanelPlugin.UI_VERSION
                + " at core=" + mountParent.getClass().getSimpleName()
                + " panel=" + panelW + "x" + panelH
                + " screen=" + screenW + "x" + screenH
                + " contentMax=" + maxGridW + "x" + maxGridH);
        installed = true;
    }

    private final java.util.Map<UIPanelAPI, float[]> hiddenSiblingState = new java.util.HashMap<UIPanelAPI, float[]>();

    private void hideSiblings(UIPanelAPI parent, UIPanelAPI keep) {
        hiddenSiblingState.clear();
        try {
            for (Object sib : ReflectionUtils.getChildrenCopy(parent)) {
                if (!(sib instanceof UIPanelAPI)) continue;
                UIPanelAPI s = (UIPanelAPI) sib;
                if (s == keep) continue;
                com.fs.starfarer.api.ui.PositionAPI sp = s.getPosition();
                if (sp == null) continue;
                hiddenSiblingState.put(s, new float[]{sp.getX(), sp.getY(), sp.getWidth(), sp.getHeight()});
                try {
                    sp.setSuspendRecompute(true);
                    sp.setSize(0.1f, 0.1f);
                    sp.setLocation(-7000f, -7000f);
                    sp.setSuspendRecompute(false);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            Global.getLogger(CharacterTabReplacer.class).warn(LOG_PREFIX + "hideSiblings failed", t);
        }
    }

    private void restoreSiblings() {
        for (java.util.Map.Entry<UIPanelAPI, float[]> e : hiddenSiblingState.entrySet()) {
            try {
                com.fs.starfarer.api.ui.PositionAPI sp = e.getKey().getPosition();
                float[] v = e.getValue();
                sp.setSuspendRecompute(true);
                sp.setSize(v[2], v[3]);
                sp.setLocation(v[0], v[1]);
                sp.setSuspendRecompute(false);
            } catch (Throwable ignored) {}
        }
        hiddenSiblingState.clear();
    }

    private UIPanelAPI findLargestPanelWithCanReassign(UIPanelAPI panel) {
        if (panel == null) return null;
        UIPanelAPI best = null;
        float bestArea = -1f;
        List<Object> children = ReflectionUtils.getChildrenCopy(panel);
        for (Object child : children) {
            if (child == null) continue;
            boolean hasMethod = ReflectionUtils.hasMethodOfName("canReassign", child);
            if (hasMethod && child instanceof UIPanelAPI) {
                UIPanelAPI candidate = (UIPanelAPI) child;
                com.fs.starfarer.api.ui.PositionAPI pos = candidate.getPosition();
                float area = pos.getWidth() * pos.getHeight();
                if (area > bestArea) {
                    bestArea = area;
                    best = candidate;
                }
            }
            if (child instanceof UIPanelAPI) {
                UIPanelAPI found = findLargestPanelWithCanReassign((UIPanelAPI) child);
                if (found != null) {
                    com.fs.starfarer.api.ui.PositionAPI pos = found.getPosition();
                    float area = pos.getWidth() * pos.getHeight();
                    if (area > bestArea) {
                        bestArea = area;
                        best = found;
                    }
                }
            }
        }
        return best;
    }

    private void maybeUninstall() {
        if (!installed) return;
        try {
            // Remove our custom panel from the core UI.
            if (mountParent != null && ourPanel != null) {
                try { mountParent.removeComponent(ourPanel); } catch (Throwable ignored) {}
                ourPanel = null;
            }
            // Restore the vanilla skill panel to its original parent/position.
            if (vanillaParent != null && vanillaPanel != null) {
                try { vanillaParent.addComponent(vanillaPanel); } catch (Throwable ignored) {}
                com.fs.starfarer.api.ui.PositionAPI tPos = vanillaPanel.getPosition();
                try {
                    tPos.setSuspendRecompute(true);
                    tPos.setSize(vanillaOrigW, vanillaOrigH);
                    tPos.setLocation(vanillaOrigX, vanillaOrigY);
                    tPos.setSuspendRecompute(false);
                } catch (Throwable ignored) {
                    tPos.setSize(vanillaOrigW, vanillaOrigH);
                    tPos.setLocation(vanillaOrigX, vanillaOrigY);
                }
            }
            // Restore any siblings we pushed off-screen.
            restoreSiblings();
        } catch (Throwable t) {
            Global.getLogger(CharacterTabReplacer.class).warn(LOG_PREFIX + "uninstall failed", t);
        }
        installed = false;
        vanillaParent = null;
        vanillaPanel = null;
        mountParent = null;
    }
}
