package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class CorsairT0Handler implements EffectHandler {

    public static final String K_PURCHASED   = "$skills.corsair.t0.purchased";
    public static final String K_CORE_ACTIVE = "$skills.corsair.t0.coreActive";
    public static final String K_BM_DISCOUNT = "$skills.corsair.bm_discount";

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
            // Null-guarded like the other fleet handlers (no player fleet yet on early load).
            if (Global.getSector().getPlayerFleet() != null) {
                Global.getSector().getPlayerFleet().getStats().getDetectedRangeMod()
                        .modifyMult(nodeId + ":base", 0.90f);
            }
            Global.getSector().getMemoryWithoutUpdate().set(K_BM_DISCOUNT, 0.05f);
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_CORE_ACTIVE, true);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
            if (Global.getSector().getPlayerFleet() != null) {
                Global.getSector().getPlayerFleet().getStats().getDetectedRangeMod().unmodify(nodeId + ":base");
            }
            Global.getSector().getMemoryWithoutUpdate().unset(K_BM_DISCOUNT);
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_CORE_ACTIVE);
        }
    }
}
