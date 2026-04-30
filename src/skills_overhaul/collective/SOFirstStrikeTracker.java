package skills_overhaul.collective;

import java.util.HashMap;
import java.util.Map;

public final class SOFirstStrikeTracker {

    // FSM is one-way by design: First Strike fires once per ship per battle.
    public enum FSState { UNENGAGED, STRUCK, DECAYING }

    private static final float DECAY_DURATION = 30f;

    private final Map<String, FSState> states  = new HashMap<String, FSState>();
    private final Map<String, Float>   elapsed = new HashMap<String, Float>();

    public FSState getState(String shipId) {
        FSState s = states.get(shipId);
        return s != null ? s : FSState.UNENGAGED;
    }

    public void notifyContact(String shipId) {
        if (getState(shipId) != FSState.UNENGAGED) return;
        states.put(shipId, FSState.STRUCK);
        elapsed.put(shipId, 0f);
    }

    public void advance(String shipId, float amount) {
        FSState s = getState(shipId);
        if (s == FSState.UNENGAGED) return;
        float e = getDecayElapsed(shipId) + amount;
        elapsed.put(shipId, e);
        // transition STRUCK → DECAYING on first advance
        if (s == FSState.STRUCK) {
            states.put(shipId, FSState.DECAYING);
        }
    }

    /** Returns bonus multiplier (0..0.30) to be applied as modifyPercent * 100. */
    public float getDamageMultiplier(String shipId) {
        FSState s = getState(shipId);
        if (s == FSState.UNENGAGED) return 0f;
        float e = getDecayElapsed(shipId);
        float bonus = 0.30f * (1f - e / DECAY_DURATION);
        return Math.max(0f, Math.min(0.30f, bonus));
    }

    public float getDecayElapsed(String shipId) {
        Float e = elapsed.get(shipId);
        return e != null ? e : 0f;
    }
}
