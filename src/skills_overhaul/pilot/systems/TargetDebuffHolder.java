package skills_overhaul.pilot.systems;

/**
 * Remembers which ship carries a debuff this lane placed on it, so the debuff
 * can be removed when the player's target changes or is lost.
 */
public final class TargetDebuffHolder<T> {
    private T held;

    /** Marks {@code target} as carrying the debuff. */
    public void hold(T target) { held = target; }

    /**
     * Returns the ship whose debuff must be removed now that the target is
     * {@code current} (null when nothing needs removing), and forgets it.
     */
    public T release(T current) {
        if (held == null || held == current) return null;
        T out = held;
        held = null;
        return out;
    }
}
