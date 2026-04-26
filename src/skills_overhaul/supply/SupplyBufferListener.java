package skills_overhaul.supply;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * Periodic reconciler: each tick, observes the player fleet's supply count.
 * On an increase, treats the delta as income, applies the +10% bonus to the delta,
 * routes through SupplyBufferService.add, and adds any overflow back to fleet cargo.
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

            // +10% bonus on the income delta; granted on top of the observed gain.
            float bonus = delta * (SupplyBufferService.INCOME_BONUS_MULTIPLIER - 1f);
            if (bonus > 0f) cargo.addSupplies(bonus);

            float total = delta + bonus;

            // Move as much of `total` into the buffer as fits; remainder stays in fleet cargo.
            float overflow = SupplyBufferService.add(mem, fleetCap, total);
            float intoBuffer = total - overflow;
            if (intoBuffer > 0f) cargo.removeSupplies(intoBuffer);

            now = cargo.getCommodityQuantity("supplies");
        }
        lastSeenSupplies = now;
    }
}
