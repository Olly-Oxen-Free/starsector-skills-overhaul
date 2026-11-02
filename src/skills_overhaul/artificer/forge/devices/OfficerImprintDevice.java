package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import skills_overhaul.artificer.ArtificerCombatPlugin;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

/**
 * Officer Imprint — imprints officer skill patterns onto carrier fighter wings.
 * Deploy sets a 7-day campaign flag; while live, {@link ArtificerCombatPlugin}
 * buffs fighter wings whose carrier has an officer (+2% damage & speed per
 * officer level, capped at +20%). Duration matches the recipe's 7-day cooldown.
 */
public final class OfficerImprintDevice implements ForgeDeviceEffect {

    @Override
    public void deployCampaign() {
        try {
            int expire = skills_overhaul.artificer.alphastorm.AlphaStormAbility.monotonicDay() + 7;
            Global.getSector().getMemoryWithoutUpdate().set(
                    ArtificerCombatPlugin.OFFICER_IMPRINT_EXPIRE_KEY, expire);
            Global.getLogger(OfficerImprintDevice.class).info(
                    "[OfficerImprint] fighter-wing imprint active for 7 days");
        } catch (Throwable t) {
            Global.getLogger(OfficerImprintDevice.class).warn(
                    "[OfficerImprint] deploy failed: " + t.getMessage());
        }
    }
}
