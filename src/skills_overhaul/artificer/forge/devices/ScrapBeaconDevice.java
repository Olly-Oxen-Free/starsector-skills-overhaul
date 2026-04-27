package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;
import skills_overhaul.supply.SupplyBufferService;

public final class ScrapBeaconDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        Global.getSector().addTransientScript(new EveryFrameScript() {
            final IntervalUtil interval = new IntervalUtil(2f, 2f);  // 2 in-game days
            float remainingDays = 30f;

            @Override public boolean isDone() { return remainingDays <= 0f; }
            @Override public boolean runWhilePaused() { return false; }
            @Override public void advance(float amount) {
                interval.advance(amount);
                if (!interval.intervalElapsed()) return;
                CampaignFleetAPIWrap.add10Supplies();
                remainingDays -= 2f;
            }
        });
    }

    static class CampaignFleetAPIWrap {
        static void add10Supplies() {
            float fleetCap = Global.getSector().getPlayerFleet().getCargo().getMaxCapacity();
            SupplyBufferService.add(
                    Global.getSector().getMemoryWithoutUpdate(), fleetCap, 10f);
        }
    }
}
