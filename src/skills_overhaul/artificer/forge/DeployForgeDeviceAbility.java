package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.List;

public final class DeployForgeDeviceAbility extends BaseDurationAbility {

    public static final String ABILITY_ID = "skills_overhaul_deploy_forge_device";

    @Override
    protected void activateImpl() {
        List<String> held = ForgeService.heldRecipes(ForgeDeviceType.CAMPAIGN);
        if (held.isEmpty()) {
            Global.getLogger(DeployForgeDeviceAbility.class).info(
                    "[Artificer] Deploy: no campaign devices held");
            this.deactivate();
            return;
        }
        String recipeId = held.get(0);
        ForgeService.deployCampaign(recipeId);
        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public void createTooltip(TooltipMakerAPI t, boolean expanded) {
        t.addTitle("Deploy Forge Device");
        t.addPara("Deploy a held campaign-type Forge device.", 5f);
        List<String> held = ForgeService.heldRecipes(ForgeDeviceType.CAMPAIGN);
        t.addPara("Held: %s campaign devices in cargo", 3f,
                Misc.getHighlightColor(),
                String.valueOf(held.size()));
    }

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        return !ForgeService.heldRecipes(ForgeDeviceType.CAMPAIGN).isEmpty();
    }
}
