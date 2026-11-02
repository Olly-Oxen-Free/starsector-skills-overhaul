package skills_overhaul.supply;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public final class ForgeCampaignBuffsTicker implements EveryFrameScript {

    private static final String KEY_SLIPSTREAM_ACTIVE  = "$skills.artificer.forge.slipstream_seeder_active";
    private static final String KEY_SLIPSTREAM_EXPIRE  = "$skills.artificer.forge.slipstream_seeder_expire";
    private static final String KEY_NAVBEACON_ACTIVE   = "$skills.artificer.forge.nav_beacon_active";
    private static final String KEY_NAVBEACON_EXPIRE   = "$skills.artificer.forge.nav_beacon_expire";
    private static final String KEY_HYPERCLOUD_ACTIVE  = "$skills.artificer.forge.hypercloud_active";
    private static final String KEY_HYPERCLOUD_EXPIRE  = "$skills.artificer.forge.hypercloud_expire";

    // Per-entity deployed devices (one memory key per spawned entity, keyed by entity id).
    private static final String PREFIX_SENSOR_BEACON = "$skills.artificer.forge.sensor_beacon_expire.";
    private static final String PREFIX_COMM_JAMMER   = "$skills.artificer.forge.comm_jammer_expire.";
    private static final float SENSOR_BEACON_RADIUS  = 2000f;
    private static final float COMM_JAMMER_RADIUS    = 2000f;

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

        processDeployedEntities(fleet, stats, today);
    }

    /**
     * Per-entity deployed campaign devices. Each spawned Sensor Beacon / Comm Jammer records a
     * <code>..._expire.&lt;entityId&gt;</code> memory key. When expired we remove the map entity and clear
     * the key; while alive, if the player fleet is in the same location and within radius:
     *  - Sensor Beacon: +50% fleet sensor range (map-intel boost).
     *  - Comm Jammer:  x0.75 fleet sensor profile (harder for enemies to detect the player — the closest
     *    campaign-API analogue to jamming enemy detection/patrol response around the device).
     */
    private void processDeployedEntities(CampaignFleetAPI fleet, MutableFleetStatsAPI stats, int today) {
        boolean sensorInRange = false;
        boolean jammerInRange = false;

        // Copy keys — removing entities/keys mutates memory during iteration.
        List<String> keys = new ArrayList<String>(
                Global.getSector().getMemoryWithoutUpdate().getKeys());
        for (String key : keys) {
            boolean isSensor = key.startsWith(PREFIX_SENSOR_BEACON);
            boolean isJammer = key.startsWith(PREFIX_COMM_JAMMER);
            if (!isSensor && !isJammer) continue;

            String prefix = isSensor ? PREFIX_SENSOR_BEACON : PREFIX_COMM_JAMMER;
            String entityId = key.substring(prefix.length());
            Object expObj = Global.getSector().getMemoryWithoutUpdate().get(key);
            SectorEntityToken entity = Global.getSector().getEntityById(entityId);

            boolean expired = !(expObj instanceof Number) || today >= ((Number) expObj).intValue();
            if (expired) {
                if (entity != null && entity.getContainingLocation() != null) {
                    entity.getContainingLocation().removeEntity(entity);
                }
                Global.getSector().getMemoryWithoutUpdate().unset(key);
                continue;
            }

            // Alive — apply effect only if the fleet is co-located and within radius.
            if (entity == null || entity.getContainingLocation() != fleet.getContainingLocation()) continue;
            float dist = Misc.getDistance(fleet.getLocation(), entity.getLocation());
            if (isSensor && dist <= SENSOR_BEACON_RADIUS) sensorInRange = true;
            if (isJammer && dist <= COMM_JAMMER_RADIUS) jammerInRange = true;
        }

        if (sensorInRange) stats.getSensorRangeMod().modifyPercent("so_sensor_beacon", 50f);
        else stats.getSensorRangeMod().unmodify("so_sensor_beacon");

        if (jammerInRange) stats.getSensorProfileMod().modifyMult("so_comm_jammer", 0.75f);
        else stats.getSensorProfileMod().unmodify("so_comm_jammer");
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
        // Must match the writers (forge devices): cycle*365 + day-of-year.
        // getDay() is day-of-month and runs backwards at month rollover.
        return skills_overhaul.artificer.alphastorm.AlphaStormAbility.monotonicDay();
    }
}
