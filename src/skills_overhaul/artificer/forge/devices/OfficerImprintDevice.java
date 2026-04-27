package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class OfficerImprintDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            // v1: skeleton — grant +1 officer slot or enhance existing officer skills (implementation deferred)
            // Effect: all officers gain +1 random skill point, or +1 officer deployment slot for 45 days
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.officer_imprint_active", true);
            int expire = (int) Global.getSector().getClock().getCycle() * 365
                    + (int) Global.getSector().getClock().getDay() + 45;
            Global.getSector().getMemoryWithoutUpdate().set(
                    "$skills.artificer.forge.officer_imprint_expire", expire);
            Global.getLogger(OfficerImprintDevice.class).info(
                    "[OfficerImprint] deployed; officer enhancement active for 45 days");
        } catch (Throwable t) {
            Global.getLogger(OfficerImprintDevice.class).warn(
                    "[OfficerImprint] deploy failed: " + t.getMessage());
        }
    }
}
