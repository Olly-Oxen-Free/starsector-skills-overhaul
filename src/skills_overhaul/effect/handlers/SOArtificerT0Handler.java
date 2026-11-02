package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.artificer.forge.ForgeRecipeRegistry;
import skills_overhaul.effect.EffectHandler;

public final class SOArtificerT0Handler implements EffectHandler {

    public static final String K_PURCHASED = "$skills.artificer.t0.purchased";
    public static final String K_CORE_ACTIVE = "$skills.artificer.t0.coreActive";

    @Override
    public void apply(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_PURCHASED, true);
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.t0.supply_reduction", 0.20f);
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.t0.sensor_range_bonus", 0.30f);
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().set(K_CORE_ACTIVE, true);
            ForgeRecipeRegistry.unlock("drone_wakes");
            ForgeRecipeRegistry.unlock("sensor_beacon");
            // Spec (Artificer Plan A): "Buy T0; Deploy Forge Device ability appears in campaign bar."
            skills_overhaul.effect.AbilityGrants.grant(skills_overhaul.artificer.forge.DeployForgeDeviceAbility.ABILITY_ID);
        }
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String channel = params.optString("channel", "base");
        if ("base".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_PURCHASED);
            Global.getSector().getMemoryWithoutUpdate().unset(
                    "$skills.artificer.t0.supply_reduction");
            Global.getSector().getMemoryWithoutUpdate().unset(
                    "$skills.artificer.t0.sensor_range_bonus");
        } else if ("core".equals(channel)) {
            Global.getSector().getMemoryWithoutUpdate().unset(K_CORE_ACTIVE);
            ForgeRecipeRegistry.lock("drone_wakes");
            ForgeRecipeRegistry.lock("sensor_beacon");
            skills_overhaul.effect.AbilityGrants.revoke(skills_overhaul.artificer.forge.DeployForgeDeviceAbility.ABILITY_ID);
        }
    }
}
