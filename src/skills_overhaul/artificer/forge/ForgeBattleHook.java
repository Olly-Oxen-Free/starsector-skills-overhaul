package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public final class ForgeBattleHook extends BaseCampaignEventListener {

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
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.set("$skills.artificer.forge.battle_pending." + recipeId, true);
        Object countObj = mem.get("$skills.artificer.forge.deploys_this_engagement");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        mem.set("$skills.artificer.forge.deploys_this_engagement", count + 1);
    }
}
