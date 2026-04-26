package skills_overhaul.supply;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * Periodic reconciler: each tick, observes the player fleet's supply count.
 * On an increase, routes the delta into the buffer (no bonus applied here).
 *
 * The +10% salvage bonus is applied upstream in SalvageBonusListener, which
 * inflates post-battle loot supplies before the player picks them up. That
 * bonus then flows through this reconciler as part of the normal delta.
 *
 * Decreases are ignored — consumption flows through SupplyBufferService.tryConsume from
 * the spend site, not through this listener.
 */
public final class SupplyBufferListener implements EveryFrameScript {

    private float lastSeenSupplies = -1f;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        CargoAPI cargo = fleet.getCargo();
        if (cargo == null) return;

        float now = cargo.getCommodityQuantity("supplies");
        if (lastSeenSupplies < 0f) { lastSeenSupplies = now; return; }

        float delta = now - lastSeenSupplies;
        if (delta > 0f) {
            MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
            float fleetCap = cargo.getMaxCapacity();

            // Route delta into the buffer; overflow stays in fleet cargo.
            float overflow = SupplyBufferService.add(mem, fleetCap, delta);
            float intoBuffer = delta - overflow;
            if (intoBuffer > 0f) cargo.removeSupplies(intoBuffer);

            now = cargo.getCommodityQuantity("supplies");
        }
        lastSeenSupplies = now;
    }
}
