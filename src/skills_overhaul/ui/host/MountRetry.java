package skills_overhaul.ui.host;

/**
 * Headless arm-once / deferred-retry state for mounting a view into the CHARACTER tab.
 *
 * Two triggers arm one mount sequence per tab open: the CoreUITabListener callback
 * ({@link #armFromListener()}) and a per-frame poll of the current core tab
 * ({@link #onPoll(boolean)}). Once a sequence is armed for an open, further triggers are
 * ignored until the poll sees the tab leave CHARACTER (and no sequence is pending). The
 * listener fires before the tab switch completes, so a pending sequence survives polls that
 * still report the old tab.
 *
 * The host calls {@link #tick(Attempt)} once per frame while the tab is CHARACTER. Each tick
 * runs one attempt; the sequence stops on the first attempt that returns true and gives up after
 * {@link #MAX_FRAMES} attempts, returning {@link Status#EXHAUSTED} exactly once per arming.
 *
 * Pure Java: no Starsector or GL imports, so MountRetryTest runs headless.
 */
public final class MountRetry {

    /** Maximum attempts per arming (nex4x IntelDiplomacyTabInjector MAX_RESOLVE). */
    public static final int MAX_FRAMES = 180;

    public enum Status {
        /** Not armed: nothing ran this tick. */
        IDLE,
        /** The attempt failed; another runs next tick. */
        RETRYING,
        /** The attempt returned true; the sequence is finished. */
        SUCCEEDED,
        /** The last allowed attempt failed; reported once per arming. */
        EXHAUSTED
    }

    /** One mount attempt; returns true when the sequence is finished (mounted, or nothing to do). */
    public interface Attempt {
        boolean attempt();
    }

    /** A sequence is pending: ticks run attempts. */
    private boolean armed;
    /** This tab open has already been armed; cleared when the poll sees the tab leave. */
    private boolean openConsumed;
    /** Attempts run in the current arming. */
    private int frames;
    /** The poll has reported CHARACTER since this sequence was armed. */
    private boolean sawCharacter;

    /** Listener trigger: arms a sequence unless this tab open already has one. */
    public void armFromListener() {
        arm();
    }

    /**
     * Poll trigger, called once per frame with whether the current core tab is CHARACTER.
     * CHARACTER arms a sequence unless this open already has one. Any other tab ends the open
     * (dropping a pending sequence and re-arming the next open), except while a listener-armed
     * sequence has not yet seen CHARACTER: the listener fires before the switch completes.
     */
    public void onPoll(boolean tabIsCharacter) {
        if (tabIsCharacter) {
            arm();
            if (armed) sawCharacter = true;
        } else if (!armed || sawCharacter) {
            armed = false;
            openConsumed = false;
        }
    }

    /** Runs one attempt if armed. */
    public Status tick(Attempt attempt) {
        if (!armed) return Status.IDLE;
        frames++;
        if (attempt.attempt()) {
            armed = false;
            return Status.SUCCEEDED;
        }
        if (frames >= MAX_FRAMES) {
            armed = false;
            return Status.EXHAUSTED;
        }
        return Status.RETRYING;
    }

    public boolean isArmed() { return armed; }

    /** Attempts run in the current (or last) arming. */
    public int getFrames() { return frames; }

    /** Forgets all state, as on a fresh instance. */
    public void reset() {
        armed = false;
        openConsumed = false;
        sawCharacter = false;
        frames = 0;
    }

    private void arm() {
        if (openConsumed) return;
        openConsumed = true;
        armed = true;
        sawCharacter = false;
        frames = 0;
    }
}
