package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;

public final class CommJammerDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            LocationAPI loc = Global.getSector().getPlayerFleet().getContainingLocation();
            Vector2f pos = Global.getSector().getPlayerFleet().getLocation();
            CustomCampaignEntityAPI jammer = loc.addCustomEntity(
                    null, "Forge Comm Jammer", "jammer_station", null);
            jammer.setLocation(pos.x, pos.y);
            // 30-day duration: set memory key
            int expire = (int) Global.getSector().getClock().getCycle() * 365
                    + (int) Global.getSector().getClock().getDay() + 30;
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.comm_jammer_expire." + jammer.getId(), expire);
            // Flag enemy comms degradation
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.comm_jammer_active", true);
        } catch (Throwable t) {
            Global.getLogger(CommJammerDevice.class).warn(
                    "[CommJammer] spawn failed: " + t.getMessage());
        }
    }
}
