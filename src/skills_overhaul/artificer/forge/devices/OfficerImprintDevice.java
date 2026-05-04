package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class OfficerImprintDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            for (OfficerDataAPI officer : Global.getSector().getPlayerFleet()
                    .getFleetData().getOfficersCopy()) {
                if (officer.getPerson() == null) continue;
                officer.getPerson().getStats().addPoints(1);
            }
            Global.getLogger(OfficerImprintDevice.class).info(
                    "[OfficerImprint] granted +1 skill point to all officers");
        } catch (Throwable t) {
            Global.getLogger(OfficerImprintDevice.class).warn(
                    "[OfficerImprint] deploy failed: " + t.getMessage());
        }
    }
}
