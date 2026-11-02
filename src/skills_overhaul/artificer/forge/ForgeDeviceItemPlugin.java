package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONException;
import java.util.Random;

public final class ForgeDeviceItemPlugin implements SpecialItemPlugin {

    private String id;
    private com.fs.starfarer.api.campaign.SpecialItemSpecAPI spec;

    @Override
    public void setId(String id) {
        this.id = id;
        this.spec = com.fs.starfarer.api.Global.getSettings().getSpecialItemSpec(id);
    }

    @Override
    public void init(CargoStackAPI stack) {
        // No initialization needed
    }

    @Override
    public String getName() {
        return "Forge Device";
    }

    @Override
    public int getPrice(com.fs.starfarer.api.campaign.econ.MarketAPI market,
                        com.fs.starfarer.api.campaign.econ.SubmarketAPI submarket) {
        return 500;
    }

    @Override
    public boolean hasRightClickAction() {
        return false;
    }

    @Override
    public void performRightClickAction() {
        // No action
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        // No action
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return false;
    }

    @Override
    public boolean isTooltipExpandable() {
        return false;
    }

    @Override
    public float getTooltipWidth() {
        return 300f;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded,
                              CargoTransferHandlerAPI handler, Object stack) {
        if (!(stack instanceof CargoStackAPI)) return;
        CargoStackAPI cargoStack = (CargoStackAPI) stack;

        SpecialItemData data = cargoStack.getSpecialDataIfSpecial();
        if (data == null) return;
        String recipeId = data.getData();
        ForgeDeviceRecipe r = ForgeRecipeRegistry.get(recipeId);
        if (r == null) {
            tooltip.addTitle("Forge Device (unknown)");
            return;
        }
        tooltip.addTitle(r.displayName);
        tooltip.addPara("%s", 5f, Misc.getHighlightColor(),
                r.type == ForgeDeviceType.BATTLE ? "BATTLE" : "CAMPAIGN");
        tooltip.addPara(r.descriptionShort, 3f);
        tooltip.addPara("Cost: %s supplies | Cooldown: %sd", 3f,
                Misc.getHighlightColor(),
                String.valueOf((int) r.supplyCost), String.valueOf(r.cooldownDays));
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult,
                       float glowMult, SpecialItemRendererAPI renderer) {
        // Default rendering
    }

    @Override
    public String resolveDropParamsToSpecificItemData(String dropParams, Random random)
            throws JSONException {
        return dropParams;
    }

    @Override
    public String getDesignType() {
        return "Forge Device";
    }

    @Override
    public com.fs.starfarer.api.campaign.SpecialItemSpecAPI getSpec() {
        return spec;
    }
}
