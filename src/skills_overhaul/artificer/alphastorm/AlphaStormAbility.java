package skills_overhaul.artificer.alphastorm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import skills_overhaul.artificer.forge.FreeDeployQuotaTracker;

public final class AlphaStormAbility extends BaseDurationAbility {

    public static final String ABILITY_ID = "skills_overhaul_alpha_storm";
    public static final String QUOTA_BONUS_KEY = "alpha_storm_active";

    @Override
    protected void activateImpl() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int today = monotonicDay();

        if (!AlphaStormState.isActive(mem)) {
            // Deactivate Sustained Burn first
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            try {
                AbilityPlugin sb = fleet.getAbility(Abilities.SUSTAINED_BURN);
                if (sb != null && sb.isActive()) sb.deactivate();
            } catch (Throwable t) {}
            AlphaStormState.start(mem, today);
            FreeDeployQuotaTracker.registerBonus(QUOTA_BONUS_KEY, 1);
        } else {
            AlphaStormState.stop(mem);
            FreeDeployQuotaTracker.unregisterBonus(QUOTA_BONUS_KEY);
        }
        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        // Mutually exclusive with Sustained Burn
        try {
            AbilityPlugin sb = Global.getSector().getPlayerFleet().getAbility(Abilities.SUSTAINED_BURN);
            if (sb != null && sb.isActive()) return false;
        } catch (Throwable t) {}
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI t, boolean expanded) {
        t.addTitle("Alpha Storm");
        t.addPara("Generates an Alpha Storm enveloping the fleet. Replaces Sustained Burn. Consumes supplies daily; intensity ramps over 7 days.", 5f);
    }

    /**
     * Monotonic campaign day counter. Uses DAY_OF_YEAR — {@code getClock().getDay()} is the
     * day of the MONTH, which made this value drop ~30 at every month rollover.
     */
    public static int monotonicDay() {
        return (int) Global.getSector().getClock().getCycle() * 365
                + Global.getSector().getClock().getCal().get(java.util.Calendar.DAY_OF_YEAR);
    }
}
