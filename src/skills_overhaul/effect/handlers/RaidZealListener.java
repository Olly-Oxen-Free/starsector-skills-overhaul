package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.TempData;
import skills_overhaul.zealot.ZealDailyTicker;
import skills_overhaul.zealot.ZealService;

import java.util.Map;

/**
 * Grants +1 Zeal stack per completed player raid (valuables or disrupt) while the
 * Heavy Detachments node is owned. Bombardments grant nothing.
 */
public final class RaidZealListener implements ColonyPlayerHostileActListener {

    /** Headless-testable gate and grant. Returns true when a stack was granted. */
    static boolean grantIfOwned(Map<String, Object> mem, int today) {
        if (!Boolean.TRUE.equals(mem.get(SOHeavyDetachmentsHandler.K_PURCHASED))) return false;
        ZealService.addStacks(mem, 1, today);
        return true;
    }

    private static void grant() {
        grantIfOwned(new ZealDailyTicker.MemoryWrapMap(Global.getSector().getMemoryWithoutUpdate()),
                ZealDailyTicker.monotonicDay());
    }

    @Override
    public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog,
            MarketAPI market, TempData actionData, CargoAPI cargo) { grant(); }

    @Override
    public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market,
            TempData actionData, Industry industry) { grant(); }

    @Override
    public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market,
            TempData actionData) { }

    @Override
    public void reportSaturationBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market,
            TempData actionData) { }
}
