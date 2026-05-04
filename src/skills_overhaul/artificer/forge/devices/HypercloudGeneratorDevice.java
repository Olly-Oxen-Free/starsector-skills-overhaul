package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class HypercloudGeneratorDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        int expire = currentDay() + 60;
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.hypercloud_expire", expire);
        Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.hypercloud_active", true);
    }

    private int currentDay() {
        return (int)(Global.getSector().getClock().getCycle() * 365
                   + Global.getSector().getClock().getDay());
    }
}
