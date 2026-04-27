package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

public final class SOMobileArsenalHandler implements EffectHandler {

    public static final String K_PURCHASED = "$skills.zealot.mobile_arsenal.purchased";

    @Override
    public void apply(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
        // Cargo capacity +20% (memory flag — no API method for fleet cargo mod)
        Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.mobile_arsenal.cargo_mult", 1.20f);
        // Supply consumption -20% (memory flag fallback since getSuppliesPerMonthMod doesn't exist)
        Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.mobile_arsenal.supply_reduction", 0.20f);
        // Post-battle CR/hull restore +10% (memory flag for future post-battle listener)
        Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.mobile_arsenal.post_battle_cr", 0.10f);
        Global.getSector().getMemoryWithoutUpdate().set("$skills.zealot.mobile_arsenal.post_battle_hull", 0.10f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.mobile_arsenal.cargo_mult");
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.mobile_arsenal.supply_reduction");
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.mobile_arsenal.post_battle_cr");
        Global.getSector().getMemoryWithoutUpdate().unset("$skills.zealot.mobile_arsenal.post_battle_hull");
    }
}
