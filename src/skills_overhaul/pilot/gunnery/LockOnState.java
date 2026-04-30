package skills_overhaul.pilot.gunnery;

public final class LockOnState {
    private static final float LOCK_TIME = 3f;
    private float timer = 0f;
    private String lastTargetId = null;
    private boolean locked = false;

    public void advance(float dt, String targetId) {
        if (targetId == null) {
            timer = 0f;
            locked = false;
            lastTargetId = null;
            return;
        }
        if (!targetId.equals(lastTargetId)) {
            timer = 0f;
            locked = false;
            lastTargetId = targetId;
        }
        timer += dt;
        if (timer >= LOCK_TIME) locked = true;
    }

    public boolean isLocked() { return locked; }
    public void reset() { timer = 0f; locked = false; lastTargetId = null; }
}
