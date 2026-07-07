package skills_overhaul.supply;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * Fills the scrap buffer from post-battle salvage: a fraction
 * (SALVAGE_TO_BUFFER_RATIO) of salvaged supplies is mirrored into the buffer
 * as virtual scrap. The loot itself is untouched — the player keeps every
 * salvaged unit AND gains scrap. Purchased/transferred supplies never feed
 * the buffer (the old cargo-delta reconciler siphoned market buys).
 *
 * Gated on commander progression via SupplyBufferService.isUnlocked.
 *
 * Note: ruins / derelict / colony-recovery salvage may use different paths and is
 * not covered here yet. Confirm coverage in-game; extend with additional hooks if
 * those salvage types miss the fill.
 */
public final class SalvageBonusListener extends BaseCampaignEventListener {

    private static final String COMMODITY_SUPPLIES = "supplies";

    public SalvageBonusListener() { super(false); }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin context, CargoAPI loot) {
        if (loot == null) return;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (!SupplyBufferService.isUnlocked(mem)) return;

        float supplies = loot.getCommodityQuantity(COMMODITY_SUPPLIES);
        if (supplies <= 0f) return;

        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        float fleetCap = (fleet != null && fleet.getCargo() != null)
                ? fleet.getCargo().getMaxCapacity() : 0f;
        float gain = supplies * SupplyBufferService.SALVAGE_TO_BUFFER_RATIO;
        SupplyBufferService.add(mem, fleetCap, gain); // overflow simply lost — it's a capped pool
    }
}
