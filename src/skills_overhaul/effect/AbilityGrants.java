package skills_overhaul.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Grants/revokes the mod's campaign abilities from their owning node handlers.
 *
 * abilities.csv has unlockedAtStart=FALSE for all of them, so a skill node is the
 * only way in. Grant goes to both character data and the player fleet (same as
 * other mods that add abilities mid-game). On load, NodeApplyService calls
 * beginReapply() before re-applying owned nodes and revokeUngranted() after, so
 * abilities from an unowned/inactive node — or from the old unlockedAtStart=TRUE
 * rows — are removed from existing saves.
 */
public final class AbilityGrants {

    private static final Set<String> MANAGED = new LinkedHashSet<>(Arrays.asList(
            skills_overhaul.corsair.UndergroundIntelAbility.ABILITY_ID,
            skills_overhaul.corsair.LetterOfMarqueAbility.ABILITY_ID,
            skills_overhaul.commandant.CraftSuppliesAbility.ABILITY_ID,
            skills_overhaul.commandant.CraftFuelAbility.ABILITY_ID,
            skills_overhaul.zealot.DesignateRivalAbility.ABILITY_ID,
            skills_overhaul.zealot.CrusadeAbility.ABILITY_ID,
            skills_overhaul.artificer.forge.DeployForgeDeviceAbility.ABILITY_ID,
            skills_overhaul.artificer.alphastorm.AlphaStormAbility.ABILITY_ID));

    private static final Set<String> grantedThisPass = new HashSet<>();

    public static void beginReapply() {
        grantedThisPass.clear();
    }

    public static void grant(String abilityId) {
        grantedThisPass.add(abilityId);
        if (Global.getSector() == null) return;
        if (!Global.getSector().getCharacterData().getAbilities().contains(abilityId)) {
            Global.getSector().getCharacterData().addAbility(abilityId);
        }
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null && !fleet.hasAbility(abilityId)) fleet.addAbility(abilityId);
    }

    public static void revoke(String abilityId) {
        grantedThisPass.remove(abilityId);
        if (Global.getSector() == null) return;
        if (Global.getSector().getCharacterData().getAbilities().contains(abilityId)) {
            Global.getSector().getCharacterData().removeAbility(abilityId);
        }
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null && fleet.hasAbility(abilityId)) fleet.removeAbility(abilityId);
    }

    /** Revoke every managed ability no owned node granted during this reapply pass. */
    public static void revokeUngranted() {
        for (String id : MANAGED) {
            if (!grantedThisPass.contains(id)) revoke(id);
        }
    }

    private AbilityGrants() {}
}
