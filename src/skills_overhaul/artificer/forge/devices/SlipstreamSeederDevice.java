package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class SlipstreamSeederDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            LocationAPI loc = Global.getSector().getPlayerFleet().getContainingLocation();
            // v1: skeleton — spawn slipstream via custom entity or terrain system (implementation deferred)
            // Effect: player fleet movement speed +30% for 45 days within location
            int expire = (int) Global.getSector().getClock().getCycle() * 365
                    + (int) Global.getSector().getClock().getDay() + 45;
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.slipstream_seeder_expire", expire);
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.slipstream_seeder_active", true);
            Global.getLogger(SlipstreamSeederDevice.class).info(
                    "[SlipstreamSeeder] activated for location; +30% movement for 45 days");
        } catch (Throwable t) {
            Global.getLogger(SlipstreamSeederDevice.class).warn(
                    "[SlipstreamSeeder] deploy failed: " + t.getMessage());
        }
    }
}
