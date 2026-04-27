package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class HypercloudGeneratorDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            LocationAPI loc = Global.getSector().getPlayerFleet().getContainingLocation();
            // v1: skeleton — spawn hypercloud visual (implementation deferred)
            // Effect: FTL cooldown -50% jumps for 60 days, FTL cost reduced
            int expire = (int) Global.getSector().getClock().getCycle() * 365
                    + (int) Global.getSector().getClock().getDay() + 60;
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.hypercloud_expire", expire);
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.hypercloud_active", true);
            Global.getLogger(HypercloudGeneratorDevice.class).info(
                    "[Hypercloud] deployed; -50% FTL cooldown & cost for 60 days");
        } catch (Throwable t) {
            Global.getLogger(HypercloudGeneratorDevice.class).warn(
                    "[Hypercloud] deploy failed: " + t.getMessage());
        }
    }
}
