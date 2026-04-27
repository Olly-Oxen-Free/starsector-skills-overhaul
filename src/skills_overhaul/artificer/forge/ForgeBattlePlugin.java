package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.List;

public final class ForgeBattlePlugin extends BaseEveryFrameCombatPlugin {

    private boolean firstFrame = true;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (!firstFrame) return;
        firstFrame = false;

        List<String> pending = new ArrayList<>();
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        for (ForgeDeviceRecipe r : ForgeRecipeRegistry.getAllUnlocked()) {
            if (r.type != ForgeDeviceType.BATTLE) continue;
            String key = "$skills.artificer.forge.battle_pending." + r.id;
            if (Boolean.TRUE.equals(mem.get(key))) {
                pending.add(r.id);
            }
        }

        int quota = FreeDeployQuotaTracker.getQuota();
        for (int i = 0; i < pending.size(); i++) {
            String recipeId = pending.get(i);
            ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
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

        for (String recipeId : pending) {
            mem.unset("$skills.artificer.forge.battle_pending." + recipeId);
        }
        mem.set("$skills.artificer.forge.deploys_this_engagement", 0);
    }
}
