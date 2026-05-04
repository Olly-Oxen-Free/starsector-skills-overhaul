package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;

public final class ForgeCampaignBuffsTicker implements EveryFrameScript {

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        MutableFleetStatsAPI stats = fleet.getStats();
        if (stats == null) return;

        int today = dayIndex();

        // Slipstream Seeder
        boolean slipActive = Boolean.TRUE.equals(
                Global.getSector().getMemoryWithoutUpdate().get(
                        "$skills.artificer.forge.slipstream_seeder_active"));
        int slipExpire = (int) Global.getSector().getMemoryWithoutUpdate().getFloat(
                "$skills.artificer.forge.slipstream_seeder_expire");
        if (slipActive && today <= slipExpire) {
            stats.getFleetwideMaxBurnMod().modifyFlat("so_slipstream_seeder", 2f);
        } else {
            stats.getFleetwideMaxBurnMod().unmodify("so_slipstream_seeder");
            if (slipActive) {
                Global.getSector().getMemoryWithoutUpdate().unset(
                        "$skills.artificer.forge.slipstream_seeder_active");
            }
        }

        // Nav Beacon Generator
        boolean navActive = Boolean.TRUE.equals(
                Global.getSector().getMemoryWithoutUpdate().get(
                        "$skills.artificer.forge.nav_beacon_active"));
        int navExpire = (int) Global.getSector().getMemoryWithoutUpdate().getFloat(
                "$skills.artificer.forge.nav_beacon_expire");
        if (navActive && today <= navExpire) {
            stats.getSensorStrengthMod().modifyFlat("so_nav_beacon", 500f);
            stats.getSensorRangeMod().modifyFlat("so_nav_beacon_range", 500f);
        } else {
            stats.getSensorStrengthMod().unmodify("so_nav_beacon");
            stats.getSensorRangeMod().unmodify("so_nav_beacon_range");
            if (navActive) {
                Global.getSector().getMemoryWithoutUpdate().unset(
                        "$skills.artificer.forge.nav_beacon_active");
            }
        }

        // Hypercloud Generator
        boolean hyperActive = Boolean.TRUE.equals(
                Global.getSector().getMemoryWithoutUpdate().get(
                        "$skills.artificer.forge.hypercloud_active"));
        int hyperExpire = (int) Global.getSector().getMemoryWithoutUpdate().getFloat(
                "$skills.artificer.forge.hypercloud_expire");
        if (hyperActive && today <= hyperExpire) {
            stats.getFuelUseHyperMult().modifyMult("so_hypercloud", 0.5f);
            stats.getFuelUseNormalMult().modifyMult("so_hypercloud_norm", 0.75f);
        } else {
            stats.getFuelUseHyperMult().unmodify("so_hypercloud");
            stats.getFuelUseNormalMult().unmodify("so_hypercloud_norm");
            if (hyperActive) {
                Global.getSector().getMemoryWithoutUpdate().unset(
                        "$skills.artificer.forge.hypercloud_active");
            }
        }
    }

    private static int dayIndex() {
        return (int) (Global.getSector().getClock().getCycle() * 365
                + Global.getSector().getClock().getDay());
    }
}
