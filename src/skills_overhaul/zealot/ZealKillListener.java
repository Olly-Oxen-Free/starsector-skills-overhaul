package skills_overhaul.zealot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ZealKillListener extends BaseCampaignEventListener {

    public ZealKillListener() { super(false); }

    public static final Set<String> LAWFUL_FACTIONS = new HashSet<>(Arrays.asList(
            "hegemony", "diktat", "persean_league", "luddic_church", "tritachyon"));
    public static final Set<String> PIRATE_PATH_SLAVER = new HashSet<>(Arrays.asList(
            "pirates", "luddic_path"));
    public static final Set<String> ANTI_AI_FACTIONS = new HashSet<>(Arrays.asList(
            "hegemony", "luddic_church", "luddic_path", "diktat"));
    public static final Set<String> REMNANT_LIKE = new HashSet<>(Arrays.asList("remnants"));
    public static final Set<String> THREAT_LIKE = new HashSet<>(Arrays.asList("threat"));
    public static final Set<String> VOID_LIKE = new HashSet<>(Arrays.asList("void"));

    /**
     * Returns the number of Zeal stacks to award for defeating a fleet of the given faction
     * with the given fleet points, under the player's current Oath.
     */
    public static int stacksForOath(Oath oath, String factionId, float fp) {
        if (oath == null || factionId == null) return 0;
        switch (oath) {
            case BLACK_FLAG:
                if (LAWFUL_FACTIONS.contains(factionId)) return fp >= 100f ? 2 : 1;
                return 0;
            case DEFENDER:
                if (PIRATE_PATH_SLAVER.contains(factionId)) return fp >= 100f ? 2 : 1;
                if (REMNANT_LIKE.contains(factionId)) return 2;
                if (THREAT_LIKE.contains(factionId)) return 2;
                if (VOID_LIKE.contains(factionId)) return 2;
                return 0;
            case MANDATE:
                if ("tritachyon".equals(factionId)) return fp >= 100f ? 2 : 1;
                if (REMNANT_LIKE.contains(factionId)) return 2;
                return 0;
            case SINGULARITY:
                if (ANTI_AI_FACTIONS.contains(factionId)) return 1;
                return 0;
        }
        return 0;
    }

    /**
     * Capital-ship variant: used when the defeated fleet was confirmed to have a capital.
     * Currently only SINGULARITY has an elevated capital bonus (2 instead of 1).
     */
    public static int stacksForOathCapital(Oath oath, String factionId) {
        if (oath == Oath.SINGULARITY && factionId != null && ANTI_AI_FACTIONS.contains(factionId)) {
            return 2;
        }
        return stacksForOath(oath, factionId, 0f);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (!result.didPlayerWin()) return;
        EngagementResultForFleetAPI loser = result.getLoserResult();
        if (loser == null) return;
        CampaignFleetAPI loserFleet = loser.getFleet();
        if (loserFleet == null) return;

        Oath oath = OathService.getCurrentOath(Global.getSector().getMemoryWithoutUpdate());
        if (oath == null) return;

        String factionId = loserFleet.getFaction() == null ? null : loserFleet.getFaction().getId();
        float fp = loserFleet.getFleetPoints();

        int stacks = stacksForOath(oath, factionId, fp);
        if (stacks > 0) {
            int today = ZealDailyTicker.monotonicDay();
            ZealService.addStacks(Global.getSector().getMemoryWithoutUpdate(), stacks, today);
        }
    }
}
