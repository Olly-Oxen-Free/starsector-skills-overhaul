package skills_overhaul.ui.model;

/**
 * Display state of one skill node: OWNED, PENDING, UNLOCKABLE, or BLOCKED with a
 * reason (PREREQ, TIER, or BUDGET with the number of points short).
 */
public final class NodeStatus {

    public enum State { OWNED, PENDING, UNLOCKABLE, BLOCKED }

    public enum Reason { PREREQ, TIER, BUDGET }

    public static final NodeStatus OWNED      = new NodeStatus(State.OWNED, null, 0);
    public static final NodeStatus PENDING    = new NodeStatus(State.PENDING, null, 0);
    public static final NodeStatus UNLOCKABLE = new NodeStatus(State.UNLOCKABLE, null, 0);

    public final State state;
    /** Set only when {@link #state} is BLOCKED. */
    public final Reason reason;
    /** Points short; non-zero only for BLOCKED(BUDGET). */
    public final int shortBy;

    private NodeStatus(State state, Reason reason, int shortBy) {
        this.state = state;
        this.reason = reason;
        this.shortBy = shortBy;
    }

    public static NodeStatus blocked(Reason reason) {
        return new NodeStatus(State.BLOCKED, reason, 0);
    }

    public static NodeStatus budget(int shortBy) {
        return new NodeStatus(State.BLOCKED, Reason.BUDGET, shortBy);
    }

    public boolean isBlocked() { return state == State.BLOCKED; }

    @Override
    public String toString() {
        if (state != State.BLOCKED) return state.name();
        return reason == Reason.BUDGET ? "BLOCKED(BUDGET short " + shortBy + ")"
                                       : "BLOCKED(" + reason + ")";
    }
}
