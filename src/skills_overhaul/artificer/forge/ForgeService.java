package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.supply.SupplyBufferService;

import java.util.ArrayList;
import java.util.List;

public final class ForgeService {

    public static final String SPECIAL_ITEM_ID = "forge_device";
    public static final String K_CD_PREFIX = "$skills.artificer.forge.cd.";
    public static final String K_FAB_INACTIVE = "$skills.artificer.forge.fab_inactive";

    public static boolean cooldownActive(long cooldownEnd, long now) {
        return cooldownEnd > 0 && cooldownEnd > now;
    }

    public static boolean isFabInactive(MemoryAPI mem) {
        return Boolean.TRUE.equals(mem.get(K_FAB_INACTIVE));
    }

    public static long getCooldownEnd(MemoryAPI mem, String recipeId) {
        Object v = mem.get(K_CD_PREFIX + recipeId);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    public static void setCooldown(MemoryAPI mem, String recipeId, long until) {
        mem.set(K_CD_PREFIX + recipeId, until);
    }

    public static boolean craft(String recipeId) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (isFabInactive(mem)) return false;
        ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
        if (r == null || !ForgeRecipeRegistry.isUnlocked(recipeId)) return false;
        long now = Global.getSector().getClock().getTimestamp();
        if (cooldownActive(getCooldownEnd(mem, recipeId), now)) return false;

        SupplyBufferService.DrainResult dr =
                SupplyBufferService.tryConsume(mem, r.supplyCost,
                        SupplyBufferService.DrainMode.ABILITY);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = fleet.getCargo();
        float fleetSupplies = cargo.getCommodityQuantity("supplies");
        if (dr.fromFleet > fleetSupplies) {
            float curBuf = SkillMemory.getSupplyBuffer(mem);
            SkillMemory.setSupplyBuffer(mem, curBuf + dr.fromBuffer);
            return false;
        }
        if (dr.fromFleet > 0f) cargo.removeCommodity("supplies", dr.fromFleet);

        SpecialItemData data = new SpecialItemData(SPECIAL_ITEM_ID, recipeId);
        cargo.addSpecial(data, 1);
        if (r.cooldownDays > 0) {
            long cdEnd = now + r.cooldownDays * 86_400_000L;
            setCooldown(mem, recipeId, cdEnd);
        }
        return true;
    }

    public static boolean deployCampaign(String recipeId) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
        if (r == null || r.type != ForgeDeviceType.CAMPAIGN) return false;
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = fleet.getCargo();
        boolean removed = false;
        for (CargoStackAPI st : cargo.getStacksCopy()) {
            SpecialItemData d = st.getSpecialDataIfSpecial();
            if (d != null && SPECIAL_ITEM_ID.equals(d.getId()) && recipeId.equals(d.getData())) {
                cargo.removeItems(st.getType(), st.getData(), 1);
                removed = true;
                break;
            }
        }
        if (!removed) return false;

        Global.getLogger(ForgeService.class).info(
                "[Artificer] CAMPAIGN deploy: " + recipeId + " (Plan C populates effect)");
        return true;
    }

    public static List<String> heldRecipes(ForgeDeviceType type) {
        List<String> out = new ArrayList<>();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null || fleet.getCargo() == null) return out;
        for (CargoStackAPI st : fleet.getCargo().getStacksCopy()) {
            SpecialItemData d = st.getSpecialDataIfSpecial();
            if (d == null || !SPECIAL_ITEM_ID.equals(d.getId())) continue;
            String recipeId = d.getData();
            ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
            if (r != null && r.type == type) {
                for (int i = 0; i < st.getSize(); i++) out.add(recipeId);
            }
        }
        return out;
    }

    private ForgeService() {}
}
