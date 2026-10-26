package skills_overhaul.ui.host;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.apache.log4j.Logger;

import skills_overhaul.ui.view.SkillTreeView;

/**
 * Puts SkillTreeView into the vanilla CHARACTER tab in place of the vanilla skill panel.
 *
 * One instance is both the CoreUITabListener and a transient EveryFrameScript. Registration
 * (5c, SkillsOverhaulModPlugin.onGameLoad) adds the same instance twice:
 * {@code sector.addTransientScript(host)} and {@code listeners.addListener(host, true)}, the
 * latter guarded by {@code hasListenerOfClass(CharTabHost.class)} as for AdminXPTracker.
 * {@code true} = transient: not saved, so a fresh instance per game load and no statics to
 * reset (all state is per instance).
 *
 * The listener callback only arms {@link MountRetry}: it fires before the tab exists. Each frame
 * {@link #advance} polls getCurrentCoreTab(), feeds the poll trigger, runs one deferred attempt
 * while the tab is CHARACTER (at most {@link MountRetry#MAX_FRAMES} per arming), and unmounts
 * when the tab leaves CHARACTER or the screen size changes (remount on the next open).
 *
 * Mount: parent.removeComponent(vanilla), then parent.addComponent(view panel) sized to the
 * parent and anchored inTL(0,0). No siblings are parked. Unmount re-adds the vanilla panel and
 * restores its size and location.
 *
 * Outcomes go to {@link HostEvents}; failed attempts log at DEBUG only.
 */
public final class CharTabHost implements EveryFrameScript, CoreUITabListener {

    private static final String LOG_PREFIX = "[SkillsOverhaul/UI] ";
    private static final Logger LOG = Global.getLogger(CharTabHost.class);

    private final HostEvents events;
    private final MountRetry retry = new MountRetry();
    private final MountRetry.Attempt attempt = new MountRetry.Attempt() {
        @Override public boolean attempt() { return tryMount(); }
    };

    /** Last probe/mount failure in the current arming, for onProbeExhausted. */
    private String lastReason;

    // Mounted state; view != null means mounted.
    private SkillTreeView view;
    private CustomPanelAPI ourPanel;
    private UIPanelAPI parent;
    private UIPanelAPI vanillaPanel;
    private float vanillaX, vanillaY, vanillaW, vanillaH;
    private float mountedScreenW, mountedScreenH;

    public CharTabHost(HostEvents events) {
        if (events == null) throw new IllegalArgumentException("events");
        this.events = events;
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }

    /** Listener trigger. Never mounts here: the tab is not built until after this returns. */
    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (tab == CoreUITabId.CHARACTER) retry.armFromListener();
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null) return;
        boolean onCharacter = ui.getCurrentCoreTab() == CoreUITabId.CHARACTER;

        if (isMounted()) {
            if (!onCharacter) {
                unmount();
            } else if (screenSizeChanged()) {
                // Remount on the next open, as CharacterTabReplacer did on a resolution change.
                unmount();
            }
        }

        retry.onPoll(onCharacter);
        if (!onCharacter) return;

        if (retry.isArmed() && retry.getFrames() == 0) lastReason = null;
        MountRetry.Status status = retry.tick(attempt);
        if (status == MountRetry.Status.EXHAUSTED) {
            events.onProbeExhausted(lastReason == null ? "UNKNOWN" : lastReason);
        }
    }

    public boolean isMounted() { return view != null; }

    /** One deferred attempt. Returns true when this arming is finished (mounted or SiC). */
    private boolean tryMount() {
        if (isMounted()) return true;
        VanillaCharTabProbe.Result r = VanillaCharTabProbe.probe();
        if (r.isSicDetected()) {
            events.onSicDetected();
            return true;
        }
        if (!r.isSuccess()) {
            lastReason = r.reason();
            LOG.debug(LOG_PREFIX + "char tab probe attempt " + retry.getFrames() + " failed: " + lastReason);
            return false;
        }
        try {
            mount(r.getParent(), r.getVanillaPanel());
            return true;
        } catch (Throwable t) {
            lastReason = "MOUNT_FAILED: " + t;
            LOG.debug(LOG_PREFIX + "char tab mount attempt " + retry.getFrames() + " failed", t);
            rollback();
            return false;
        }
    }

    private void mount(UIPanelAPI p, UIPanelAPI vanilla) {
        PositionAPI vPos = vanilla.getPosition();
        vanillaX = vPos.getX();
        vanillaY = vPos.getY();
        vanillaW = vPos.getWidth();
        vanillaH = vPos.getHeight();

        PositionAPI pPos = p.getPosition();
        float w = pPos.getWidth();
        float h = pPos.getHeight();

        parent = p;
        vanillaPanel = vanilla;
        p.removeComponent(vanilla);

        SkillTreeView v = new SkillTreeView(w, h);
        ourPanel = v.getPanel();
        p.addComponent(ourPanel).inTL(0f, 0f);
        view = v;

        mountedScreenW = Global.getSettings().getScreenWidth();
        mountedScreenH = Global.getSettings().getScreenHeight();
        LOG.debug(LOG_PREFIX + "char tab mounted in " + p.getClass().getSimpleName()
                + " size=" + w + "x" + h);
    }

    /** Undoes a partial mount after a throw so the vanilla panel is back in its parent. */
    private void rollback() {
        view = null;
        if (parent != null && ourPanel != null) {
            try { parent.removeComponent(ourPanel); } catch (Throwable ignored) {}
        }
        restoreVanilla();
        ourPanel = null;
        parent = null;
        vanillaPanel = null;
    }

    private void unmount() {
        try {
            view.discard();
        } catch (Throwable t) {
            LOG.debug(LOG_PREFIX + "char tab discard failed", t);
        }
        try {
            parent.removeComponent(ourPanel);
        } catch (Throwable t) {
            LOG.debug(LOG_PREFIX + "char tab remove of our panel failed", t);
        }
        restoreVanilla();
        view = null;
        ourPanel = null;
        parent = null;
        vanillaPanel = null;
    }

    private void restoreVanilla() {
        if (parent == null || vanillaPanel == null) return;
        try {
            parent.addComponent(vanillaPanel);
            PositionAPI pos = vanillaPanel.getPosition();
            pos.setSuspendRecompute(true);
            pos.setSize(vanillaW, vanillaH);
            pos.setLocation(vanillaX, vanillaY);
            pos.setSuspendRecompute(false);
        } catch (Throwable t) {
            LOG.debug(LOG_PREFIX + "char tab vanilla panel restore failed", t);
        }
    }

    private boolean screenSizeChanged() {
        return Global.getSettings().getScreenWidth() != mountedScreenW
                || Global.getSettings().getScreenHeight() != mountedScreenH;
    }
}
