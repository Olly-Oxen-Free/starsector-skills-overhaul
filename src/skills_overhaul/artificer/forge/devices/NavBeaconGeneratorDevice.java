package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class NavBeaconGeneratorDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        int expire = currentDay() + 30;
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.nav_beacon_expire", expire);
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.nav_beacon_active", true);
    }

    private int currentDay() {
        return (int)(Global.getSector().getClock().getCycle() * 365
                   + Global.getSector().getClock().getDay());
    }
}
