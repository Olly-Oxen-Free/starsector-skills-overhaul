package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ForgeBattlePlugin extends BaseEveryFrameCombatPlugin {

    /**
     * Set fresh each engagement: true iff an Alpha Storm Emitter was deployed this battle.
     * AlphaStormBattleScript reads this to decide whether a storm shelter exists. Written by
     * this plugin (which runs before AlphaStormBattleScript per settings.json plugin order) so
     * the flag is current before AlphaStormBattleScript's first frame.
     */
    public static final String SHELTER_ACTIVE_KEY =
            "$skills.artificer.forge.alpha_storm_shelter_active";

    private boolean firstFrame = true;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (!firstFrame) return;
        firstFrame = false;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        // Deploy order is the firing order: the first `quota` deploys are free, the rest are
        // penalized — deterministic because it follows the order the player deployed devices.
        List<String> order = drainDeployQueue(mem);

        // Fix 1 signal: the storm shelter exists only if the emitter was deployed this battle.
        mem.set(SHELTER_ACTIVE_KEY, order.contains("alpha_storm_emitter"));

        int quota = FreeDeployQuotaTracker.getQuota();
        for (int i = 0; i < order.size(); i++) {
            String recipeId = order.get(i);
            ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
            if (r == null) continue;
            ForgeDeviceEffect effect = ForgeDeviceEffectRegistry.get(r.effectHandlerKey);
            boolean overQuota = (i >= quota);
            if (effect != null) {
                effect.deployBattle(engine, overQuota);
            } else {
                Global.getLogger(ForgeBattlePlugin.class).warn(
                        "[Forge] no effect registered for " + r.effectHandlerKey);
            }
            if (overQuota && r.penaltyKey != null) {
                ForgeDevicePenalty penalty = ForgeDevicePenaltyRegistry.get(r.penaltyKey);
                if (penalty != null) {
                    ShipAPI flagship = engine.getPlayerShip();
                    penalty.apply(engine, flagship);
                }
            }
        }
    }

    /** Drain the ordered deploy queue, returning recipe ids in deploy order (runtime). */
    public static List<String> drainDeployQueue(MemoryAPI mem) {
        List<String> out = ForgeBattleHook.readOrder(mem.get(ForgeBattleHook.DEPLOY_ORDER_KEY));
        mem.unset(ForgeBattleHook.DEPLOY_ORDER_KEY);
        return out;
    }

    /** Drain the ordered deploy queue, returning recipe ids in deploy order (pure, testable). */
    public static List<String> drainDeployQueue(Map<String,Object> mem) {
        List<String> out = new ArrayList<>(ForgeBattleHook.readOrder(mem.get(ForgeBattleHook.DEPLOY_ORDER_KEY)));
        mem.remove(ForgeBattleHook.DEPLOY_ORDER_KEY);
        return out;
    }
}
