package skills_overhaul.supply;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;

public final class ForgeCampaignBuffsTicker implements EveryFrameScript {

    private static final String KEY_SLIPSTREAM_ACTIVE  = "$skills.artificer.forge.slipstream_seeder_active";
    private static final String KEY_SLIPSTREAM_EXPIRE  = "$skills.artificer.forge.slipstream_seeder_expire";
    private static final String KEY_NAVBEACON_ACTIVE   = "$skills.artificer.forge.nav_beacon_active";
    private static final String KEY_NAVBEACON_EXPIRE   = "$skills.artificer.forge.nav_beacon_expire";
    private static final String KEY_HYPERCLOUD_ACTIVE  = "$skills.artificer.forge.hypercloud_active";
    private static final String KEY_HYPERCLOUD_EXPIRE  = "$skills.artificer.forge.hypercloud_expire";

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        MutableFleetStatsAPI stats = fleet.getStats();
        int today = currentDay();

        // Slipstream Seeder: +2 burn levels while active
        if (isActive(KEY_SLIPSTREAM_ACTIVE, KEY_SLIPSTREAM_EXPIRE, today)) {
            stats.getFleetwideMaxBurnMod().modifyFlat("so_slipstream", 2f);
        } else {
            stats.getFleetwideMaxBurnMod().unmodify("so_slipstream");
        }

        // Nav Beacon: +50% sensor range while active
        if (isActive(KEY_NAVBEACON_ACTIVE, KEY_NAVBEACON_EXPIRE, today)) {
            stats.getSensorRangeMod().modifyPercent("so_navbeacon", 50f);
        } else {
            stats.getSensorRangeMod().unmodify("so_navbeacon");
        }

        // Hypercloud Generator: -50% fuel use while active
        if (isActive(KEY_HYPERCLOUD_ACTIVE, KEY_HYPERCLOUD_EXPIRE, today)) {
            stats.getFuelUseHyperMult().modifyMult("so_hypercloud", 0.5f);
            stats.getFuelUseNormalMult().modifyMult("so_hypercloud", 0.5f);
        } else {
            stats.getFuelUseHyperMult().unmodify("so_hypercloud");
            stats.getFuelUseNormalMult().unmodify("so_hypercloud");
        }
    }

    private boolean isActive(String activeKey, String expireKey, int today) {
        if (!Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate().get(activeKey))) return false;
        Object expObj = Global.getSector().getMemoryWithoutUpdate().get(expireKey);
        if (!(expObj instanceof Number)) return false;
        int expire = ((Number) expObj).intValue();
        if (today >= expire) {
            Global.getSector().getMemoryWithoutUpdate().unset(activeKey);
            Global.getSector().getMemoryWithoutUpdate().unset(expireKey);
            return false;
        }
        return true;
    }

    private int currentDay() {
        return (int)(Global.getSector().getClock().getCycle() * 365
                   + Global.getSector().getClock().getDay());
    }
}
