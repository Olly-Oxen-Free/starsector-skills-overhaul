package skills_overhaul.ui.host;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.state.AppDriver;

import skills_overhaul.ui.ReflectionUtils;
import skills_overhaul.ui.ReflectionUtils.ReflectionFailure;

import java.util.List;

/**
 * Finds the vanilla skill panel in the open CHARACTER tab, and its parent, by reflection.
 *
 * {@link #probe()} never throws and never logs: it returns either a success carrying
 * {core, vanillaPanel, parent}, a Second-in-Command detection, or a named {@link Failure}.
 * A reflective call that fails maps to {@link Failure#REFLECTION_ERROR} with its message; it
 * is never read as "no children" or "no method". All reflection goes through ReflectionUtils'
 * strict (throwing, non-logging) variants because the script classloader blocks
 * java.lang.reflect in mod code.
 */
public final class VanillaCharTabProbe {

    private VanillaCharTabProbe() {}

    /** Mod id of Second-in-Command, which replaces the same vanilla panel. */
    public static final String SIC_MOD_ID = "second_in_command";

    public enum Failure {
        /** AppDriver's current state is not a CampaignState. */
        NOT_CAMPAIGN_STATE,
        /** Neither the encounter dialog's core UI nor the state's core is a panel. */
        NO_CORE,
        /** No descendant with a canReassign method (tab not built yet, or SiC removed it). */
        NO_VANILLA_PANEL,
        /** The vanilla panel has no parent panel. */
        NO_PARENT,
        /** The parent has no usable size yet. */
        PARENT_ZERO_SIZE,
        /** A reflective call failed; see {@link Result#getDetail()}. */
        REFLECTION_ERROR
    }

    /** Outcome of one probe: success, SiC detected, or a failure reason. */
    public static final class Result {
        private final Failure failure;
        private final boolean sicDetected;
        private final String detail;
        private final UIPanelAPI core, vanillaPanel, parent;

        private Result(Failure failure, boolean sicDetected, String detail,
                       UIPanelAPI core, UIPanelAPI vanillaPanel, UIPanelAPI parent) {
            this.failure = failure;
            this.sicDetected = sicDetected;
            this.detail = detail;
            this.core = core;
            this.vanillaPanel = vanillaPanel;
            this.parent = parent;
        }

        static Result success(UIPanelAPI core, UIPanelAPI vanilla, UIPanelAPI parent) {
            return new Result(null, false, null, core, vanilla, parent);
        }
        static Result sic(UIPanelAPI core, UIPanelAPI vanilla, UIPanelAPI parent) {
            return new Result(null, true, null, core, vanilla, parent);
        }
        static Result fail(Failure f, String detail) {
            return new Result(f, false, detail, null, null, null);
        }

        /** Vanilla panel and parent found, SiC not present: the caller may mount. */
        public boolean isSuccess() { return failure == null && !sicDetected; }
        /** Second-in-Command is present: the caller must not mount. */
        public boolean isSicDetected() { return sicDetected; }
        /** Failure reason, or null on success / SiC. */
        public Failure getFailure() { return failure; }
        /** Extra text for REFLECTION_ERROR (the failed call), else null. */
        public String getDetail() { return detail; }
        public UIPanelAPI getCore() { return core; }
        public UIPanelAPI getVanillaPanel() { return vanillaPanel; }
        public UIPanelAPI getParent() { return parent; }

        /** Reason name for logs and HostEvents, e.g. "NO_PARENT" or "REFLECTION_ERROR: ...". */
        public String reason() {
            if (failure == null) return sicDetected ? "SIC_DETECTED" : "OK";
            return detail == null ? failure.name() : failure.name() + ": " + detail;
        }
    }

    /** Runs discovery once. Never throws. */
    public static Result probe() {
        try {
            if (isSicModEnabled()) return Result.sic(null, null, null);

            Object rawState = AppDriver.getInstance().getCurrentState();
            if (!(rawState instanceof CampaignState)) {
                return Result.fail(Failure.NOT_CAMPAIGN_STATE,
                        rawState == null ? "null" : rawState.getClass().getName());
            }
            CampaignState state = (CampaignState) rawState;

            UIPanelAPI core = null;
            Object dialog = ReflectionUtils.invokeStrict("getEncounterDialog", state);
            if (dialog != null) {
                Object c = ReflectionUtils.invokeStrict("getCoreUI", dialog);
                if (c instanceof UIPanelAPI) core = (UIPanelAPI) c;
            }
            if (core == null) {
                Object c = ReflectionUtils.invokeStrict("getCore", state);
                if (c instanceof UIPanelAPI) core = (UIPanelAPI) c;
            }
            if (core == null) return Result.fail(Failure.NO_CORE, null);

            UIPanelAPI vanilla = null;
            Object currentTab = ReflectionUtils.invokeStrict("getCurrentTab", core);
            if (currentTab instanceof UIPanelAPI) {
                vanilla = findLargestWithCanReassign((UIPanelAPI) currentTab);
            }
            if (vanilla == null) vanilla = findLargestWithCanReassign(core);
            if (vanilla == null) return Result.fail(Failure.NO_VANILLA_PANEL, null);

            UIPanelAPI parent = ReflectionUtils.getParentStrict(vanilla);
            if (parent == null) return Result.fail(Failure.NO_PARENT, null);

            if (hasSicSibling(parent)) return Result.sic(core, vanilla, parent);

            PositionAPI pp = parent.getPosition();
            if (pp == null || pp.getWidth() <= 0f || pp.getHeight() <= 0f) {
                return Result.fail(Failure.PARENT_ZERO_SIZE, null);
            }
            return Result.success(core, vanilla, parent);
        } catch (ReflectionFailure e) {
            return Result.fail(Failure.REFLECTION_ERROR, describe(e));
        } catch (Throwable t) {
            // Linkage or casting errors from obfuscated classes after a game update.
            return Result.fail(Failure.REFLECTION_ERROR, describe(t));
        }
    }

    /** SiC detection by mod id; robust across SiC versions. */
    public static boolean isSicModEnabled() {
        return Global.getSettings().getModManager().isModEnabled(SIC_MOD_ID);
    }

    /** Fallback SiC detection: a sibling whose class name contains "SCSkillMenu". */
    static boolean hasSicSibling(UIPanelAPI parent) throws ReflectionFailure {
        for (Object sib : ReflectionUtils.getChildrenCopyStrict(parent)) {
            if (sib != null && sib.getClass().getSimpleName().contains("SCSkillMenu")) return true;
        }
        return false;
    }

    /** Largest-area descendant that declares canReassign (the vanilla skill panel). */
    private static UIPanelAPI findLargestWithCanReassign(UIPanelAPI panel) throws ReflectionFailure {
        if (panel == null) return null;
        UIPanelAPI best = null;
        float bestArea = -1f;
        List<Object> children = ReflectionUtils.getChildrenCopyStrict(panel);
        for (Object child : children) {
            if (!(child instanceof UIPanelAPI)) continue;
            UIPanelAPI candidate = (UIPanelAPI) child;
            if (ReflectionUtils.hasMethodOfNameStrict("canReassign", child)) {
                float area = area(candidate);
                if (area > bestArea) {
                    bestArea = area;
                    best = candidate;
                }
            }
            UIPanelAPI found = findLargestWithCanReassign(candidate);
            if (found != null) {
                float area = area(found);
                if (area > bestArea) {
                    bestArea = area;
                    best = found;
                }
            }
        }
        return best;
    }

    private static float area(UIPanelAPI p) {
        PositionAPI pos = p.getPosition();
        return pos == null ? 0f : pos.getWidth() * pos.getHeight();
    }

    private static String describe(Throwable t) {
        String s = t.getClass().getSimpleName() + ": " + t.getMessage();
        Throwable c = t.getCause();
        if (c != null) s += " (" + c.getClass().getSimpleName() + ": " + c.getMessage() + ")";
        return s;
    }
}
