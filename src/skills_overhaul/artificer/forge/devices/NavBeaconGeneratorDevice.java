package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class NavBeaconGeneratorDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            LocationAPI loc = Global.getSector().getPlayerFleet().getContainingLocation();
            // v1: skeleton — spawn navigation beacon custom entity (implementation deferred)
            // Effect: unknown/hostile fleets detected at +2 sensor range for 30 days
            int expire = (int) Global.getSector().getClock().getCycle() * 365
                    + (int) Global.getSector().getClock().getDay() + 30;
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.nav_beacon_expire", expire);
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.nav_beacon_active", true);
            Global.getLogger(NavBeaconGeneratorDevice.class).info(
                    "[NavBeacon] deployed; +2 sensor range detection for 30 days");
        } catch (Throwable t) {
            Global.getLogger(NavBeaconGeneratorDevice.class).warn(
                    "[NavBeacon] deploy failed: " + t.getMessage());
        }
    }
}
