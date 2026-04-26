package skills_overhaul.supply;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.state.SkillMemory;

/**
 * HUD bar showing current/cap of the Supply Buffer.
 *
 * Integration approach TBD — research SiC's scrap-bar renderer for the hook pattern,
 * either CampaignUIPlugin overlay or a tooltip extension on the supplies indicator.
 * For now this script idles; the helper methods below expose the data the eventual
 * renderer will need.
 */
public final class SupplyBufferRenderer implements EveryFrameScript {

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }

    @Override
    public void advance(float amount) {
        // TODO: bind to CampaignUIPlugin overlay or supplies-tooltip extension.
    }

    public static float currentValue() {
        return SkillMemory.getSupplyBuffer(mem());
    }

    public static float currentCap() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        float fleetCap = (fleet != null && fleet.getCargo() != null)
                ? fleet.getCargo().getMaxCapacity() : 0f;
        return SupplyBufferService.computeCap(mem(), fleetCap);
    }

    private static MemoryAPI mem() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
