package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class SlipstreamSeederDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        int expire = currentDay() + 45;
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.slipstream_seeder_expire", expire);
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.slipstream_seeder_active", true);
    }

    private int currentDay() {
        return (int)(Global.getSector().getClock().getCycle() * 365
                   + Global.getSector().getClock().getDay());
    }
}
