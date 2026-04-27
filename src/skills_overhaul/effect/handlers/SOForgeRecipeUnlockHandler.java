package skills_overhaul.effect.handlers;

import org.json.JSONObject;
import skills_overhaul.artificer.forge.ForgeRecipeRegistry;
import skills_overhaul.effect.EffectHandler;

public final class SOForgeRecipeUnlockHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        String recipeId = params.optString("recipeId", null);
        if (recipeId != null) ForgeRecipeRegistry.unlock(recipeId);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        String recipeId = params.optString("recipeId", null);
        if (recipeId != null) ForgeRecipeRegistry.lock(recipeId);
    }
}
