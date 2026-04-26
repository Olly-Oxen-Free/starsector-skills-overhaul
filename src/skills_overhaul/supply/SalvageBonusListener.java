package skills_overhaul.supply;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;

/**
 * Inflates salvaged supplies by SupplyBufferService.INCOME_BONUS_MULTIPLIER - 1f.
 *
 * Hook: reportEncounterLootGenerated fires when post-battle salvage loot is built,
 * before the player picks it up. The bonus supplies are added directly to the loot
 * cargo, so they flow through the existing buffer reconciler the same way the
 * base salvage does.
 *
 * Note: ruins / derelict / colony-recovery salvage may use different paths and is
 * not covered here yet. Confirm coverage in-game; extend with additional hooks if
 * those salvage types miss the bonus.
 */
public final class SalvageBonusListener extends BaseCampaignEventListener {

    private static final String COMMODITY_SUPPLIES = "supplies";

    public SalvageBonusListener() { super(false); }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin context, CargoAPI loot) {
        if (loot == null) return;
        float supplies = loot.getCommodityQuantity(COMMODITY_SUPPLIES);
        if (supplies <= 0f) return;
        float bonus = supplies * (SupplyBufferService.INCOME_BONUS_MULTIPLIER - 1f);
        if (bonus > 0f) loot.addCommodity(COMMODITY_SUPPLIES, bonus);
    }
}
