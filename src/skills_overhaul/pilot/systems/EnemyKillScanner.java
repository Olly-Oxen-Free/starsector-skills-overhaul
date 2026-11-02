package skills_overhaul.pilot.systems;

import java.util.HashSet;
import java.util.Set;

/**
 * Frame-to-frame enemy kill detection for Kill Sync and Red Line. Pure Java:
 * the plugin feeds it each ship's id, owner, alive and fighter flags once per
 * frame, then calls endFrame().
 */
public final class EnemyKillScanner {
    private static final int ENEMY_OWNER = 1;

    private final Set<String> prevAlive = new HashSet<String>();
    private final Set<String> nowAlive = new HashSet<String>();

    /** Records a live enemy non-fighter without reporting kills (combat start). */
    public void seed(String id, int owner, boolean alive, boolean fighter) {
        if (owner == ENEMY_OWNER && alive && !fighter) prevAlive.add(id);
    }

    /** Returns true when this ship was a live enemy last frame and is dead now. */
    public boolean observe(String id, int owner, boolean alive, boolean fighter) {
        if (owner == ENEMY_OWNER && alive && !fighter) nowAlive.add(id);
        // No owner test: a destroyed ship is handed to the neutral owner (100),
        // and prevAlive already holds only live enemy non-fighters.
        if (fighter) return false;
        return prevAlive.contains(id) && !alive;
    }

    public void endFrame() {
        prevAlive.clear();
        prevAlive.addAll(nowAlive);
        nowAlive.clear();
    }

    public void clear() {
        prevAlive.clear();
        nowAlive.clear();
    }
}
