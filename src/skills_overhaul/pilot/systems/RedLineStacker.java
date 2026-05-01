package skills_overhaul.pilot.systems;

import java.util.ArrayList;
import java.util.List;

public final class RedLineStacker {
    private static final float STACK_DURATION = 30f;
    private static final float BONUS_PER_STACK = 0.025f;

    private final List<Float> stackTimers = new ArrayList<>();

    public void onKill() { stackTimers.add(STACK_DURATION); }

    public void advance(float dt) {
        for (int i = stackTimers.size() - 1; i >= 0; i--) {
            float newVal = stackTimers.get(i) - dt;
            if (newVal <= 0f) stackTimers.remove(i);
            else stackTimers.set(i, newVal);
        }
    }

    public int getStackCount() { return stackTimers.size(); }
    public float getFluxDissBonus() { return stackTimers.size() * BONUS_PER_STACK; }
    public float getCRBonus() { return stackTimers.size() * BONUS_PER_STACK; }
    public void reset() { stackTimers.clear(); }
}
