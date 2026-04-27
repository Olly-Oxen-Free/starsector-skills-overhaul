package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;

public final class SensorBeaconDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            LocationAPI loc = Global.getSector().getPlayerFleet().getContainingLocation();
            Vector2f pos = Global.getSector().getPlayerFleet().getLocation();
            CustomCampaignEntityAPI beacon = loc.addCustomEntity(
                    null, "Forge Sensor Beacon", "sensor_array", null);
            beacon.setLocation(pos.x, pos.y);
            // 7-day expire — schedule via memory key
            int expire = (int) Global.getSector().getClock().getCycle() * 365
                    + (int) Global.getSector().getClock().getDay() + 7;
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.sensor_beacon_expire." + beacon.getId(), expire);
        } catch (Throwable t) {
            Global.getLogger(SensorBeaconDevice.class).warn(
                    "[SensorBeacon] spawn failed: " + t.getMessage());
        }
    }
}
