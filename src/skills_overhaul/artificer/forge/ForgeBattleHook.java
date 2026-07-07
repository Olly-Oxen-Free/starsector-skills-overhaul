package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ForgeBattleHook extends BaseCampaignEventListener {

    /**
     * Ordered queue of battle-device recipe ids deployed for the upcoming engagement.
     * Deploy order is preserved (duplicates included) so ForgeBattlePlugin fires one
     * effect per deploy and assigns the free-deploy quota / penalties deterministically.
     */
    public static final String DEPLOY_ORDER_KEY = "$skills.artificer.forge.deploy_order";

    public ForgeBattleHook() { super(false); }

    public static void offerDeployBattleDevice(String recipeId) {
        if (recipeId == null) return;
        ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
        if (r == null || r.type != ForgeDeviceType.BATTLE) return;
        boolean removed = ForgeService.heldRecipes(ForgeDeviceType.BATTLE).contains(recipeId);
        if (!removed) return;
        for (CargoStackAPI st :
                Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
            SpecialItemData d = st.getSpecialDataIfSpecial();
            if (d != null && ForgeService.SPECIAL_ITEM_ID.equals(d.getId())
                    && recipeId.equals(d.getData())) {
                Global.getSector().getPlayerFleet().getCargo()
                        .removeItems(st.getType(), st.getData(), 1);
                break;
            }
        }
        recordDeploy(Global.getSector().getMemoryWithoutUpdate(), recipeId);
    }

    /** Append a deployed battle-device recipe id to the ordered deploy queue (runtime). */
    public static void recordDeploy(MemoryAPI mem, String recipeId) {
        List<String> order = readOrder(mem.get(DEPLOY_ORDER_KEY));
        order.add(recipeId);
        mem.set(DEPLOY_ORDER_KEY, order);
    }

    /** Append a deployed battle-device recipe id to the ordered deploy queue (pure, testable). */
    public static void recordDeploy(Map<String,Object> mem, String recipeId) {
        List<String> order = readOrder(mem.get(DEPLOY_ORDER_KEY));
        order.add(recipeId);
        mem.put(DEPLOY_ORDER_KEY, order);
    }

    @SuppressWarnings("unchecked")
    static List<String> readOrder(Object listObj) {
        return (listObj instanceof List) ? (List<String>) listObj : new ArrayList<String>();
    }
}
