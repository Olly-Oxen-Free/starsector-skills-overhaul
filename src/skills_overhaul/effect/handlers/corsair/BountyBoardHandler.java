package skills_overhaul.effect.handlers.corsair;

import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class BountyBoardHandler implements EffectHandler {
    @Override public void apply(String nodeId, JSONObject params) {
        BountyBoardListener.active = true;
        BountyBoardListener.weaponDropMult = 1f + (float) params.optDouble("weaponDropMult", 0.15);
    }
    @Override public void remove(String nodeId, JSONObject params) {
        BountyBoardListener.active = false;
        BountyBoardListener.weaponDropMult = 1f;
    }
}
