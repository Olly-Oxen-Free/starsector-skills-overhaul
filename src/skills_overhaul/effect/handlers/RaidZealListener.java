package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import skills_overhaul.zealot.ZealDailyTicker;
import skills_overhaul.zealot.ZealService;

/**
 * v1: stub. Vanilla raid hooks come via dialog plugins / GroundRaidIntel; the listener
 * is registered for future integration. No-op until wired to a concrete raid event.
 */
public final class RaidZealListener extends BaseCampaignEventListener {

    public RaidZealListener() { super(false); }

    public void onRaid(String targetMarketId) {
        if (!Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate()
                .get(SOHeavyDetachmentsHandler.K_PURCHASED))) return;
        int today = ZealDailyTicker.monotonicDay();
        ZealService.addStacks(Global.getSector().getMemoryWithoutUpdate(), 1, today);
    }
}
