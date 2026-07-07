package skills_overhaul.artificer.alphastorm;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.supply.SupplyBufferService;

public final class AlphaStormCampaignTicker implements EveryFrameScript {

    public static final String K_LAST_BILLED_DAY = "$skills.artificer.alpha_storm.last_billed_day";
    public static final String STAT_KEY = "skills_overhaul_alpha_storm";

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (!AlphaStormState.isActive(mem)) {
            // Cleanup any applied stats if newly inactive
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if (fleet != null) {
                fleet.getStats().getDetectedRangeMod().unmodify(STAT_KEY);
                fleet.getStats().getFleetwideMaxBurnMod().unmodify(STAT_KEY);
            }
            return;
        }

        int today = AlphaStormAbility.monotonicDay();
        AlphaStormState.Intensity intensity = AlphaStormState.intensity(mem, today);
        if (intensity == null) return;

        // Daily supply drain
        Object lastObj = mem.get(K_LAST_BILLED_DAY);
        Integer last = lastObj instanceof Number ? ((Number) lastObj).intValue() : null;
        if (last == null) {
            mem.set(K_LAST_BILLED_DAY, today);
        } else if (today > last) {
            // Bill each skipped day at that day's own intensity — a skip that crosses a
            // MILD→SEVERE boundary must not charge every day at today's (highest) rate.
            for (int day = last + 1; day <= today; day++) {
                AlphaStormState.Intensity dayIntensity = AlphaStormState.intensity(mem, day);
                if (dayIntensity == null) continue;
                int dailyCost = AlphaStormState.dailyCost(dayIntensity);
                SupplyBufferService.DrainResult dr = SupplyBufferService.tryConsume(
                        mem, dailyCost, SupplyBufferService.DrainMode.UPKEEP);
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
                CargoAPI cargo = fleet.getCargo();
                float fleetSupplies = cargo.getCommodityQuantity("supplies");
                if (dr.fromFleet > fleetSupplies) {
                    // Out of supplies — auto-deactivate (UPKEEP mode never touches the buffer).
                    AlphaStormState.stop(mem);
                    skills_overhaul.artificer.forge.FreeDeployQuotaTracker.unregisterBonus(
                            AlphaStormAbility.QUOTA_BONUS_KEY);
                    break;
                }
                if (dr.fromFleet > 0f) cargo.removeSupplies(dr.fromFleet);
            }
            mem.set(K_LAST_BILLED_DAY, today);
        }

        // Apply per-intensity fleet stats
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            float sensorReduction = 0.10f;
            float maneuverBonus = 0.10f;
            switch (intensity) {
                case MILD:     sensorReduction = 0.10f; maneuverBonus = 0.10f; break;
                case MODERATE: sensorReduction = 0.20f; maneuverBonus = 0.20f; break;
                case SEVERE:   sensorReduction = 0.25f; maneuverBonus = 0.30f; break;
            }
            fleet.getStats().getDetectedRangeMod().modifyMult(STAT_KEY, 1f - sensorReduction);
            fleet.getStats().getFleetwideMaxBurnMod().modifyFlat(STAT_KEY, maneuverBonus * 5f);
        }
    }
}
