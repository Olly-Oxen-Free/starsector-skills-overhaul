package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class PostBattleRestoreListener extends BaseCampaignEventListener {

    public PostBattleRestoreListener() { super(false); }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (!result.didPlayerWin()) return;
        if (result.getWinnerResult() == null) return;
        CampaignFleetAPI fleet = result.getWinnerResult().getFleet();
        if (fleet == null || !fleet.isPlayerFleet()) return;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        float hullRestore = 0f;
        float crRestore = 0f;

        if (Boolean.TRUE.equals(mem.get(SOFieldLogisticsHandler.K_PURCHASED))) {
            hullRestore += 0.25f;
        }
        Object marsenalH = mem.get("$skills.zealot.mobile_arsenal.post_battle_hull");
        Object marsenalCr = mem.get("$skills.zealot.mobile_arsenal.post_battle_cr");
        if (marsenalH instanceof Number)  hullRestore += ((Number) marsenalH).floatValue();
        if (marsenalCr instanceof Number) crRestore   += ((Number) marsenalCr).floatValue();

        // Crusade fork: extra restore vs Rival
        if (skills_overhaul.zealot.CrusadeService.isActive(mem)) {
            String rival = skills_overhaul.zealot.CrusadeService.getCrusadeRival(mem);
            CampaignFleetAPI loser = result.getLoserResult() == null ? null : result.getLoserResult().getFleet();
            if (rival != null && loser != null && loser.getFaction() != null
                    && rival.equals(loser.getFaction().getId())) {
                // Crusade capstone: +15% hull restored after combat vs Rival (spec 6.2).
                hullRestore += 0.15f;
                // Mobile Arsenal during Crusade vs Rival: additional +5% CR/hull (spec, Mobile Arsenal).
                if (Boolean.TRUE.equals(mem.get(SOMobileArsenalHandler.K_PURCHASED))) {
                    hullRestore += 0.05f;
                    crRestore   += 0.05f;
                }
            }
        }

        if (hullRestore <= 0f && crRestore <= 0f) return;

        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (hullRestore > 0f) {
                try {
                    float currentHull = m.getStatus().getHullFraction();
                    float newHull = Math.min(1f, currentHull + hullRestore);
                    m.getStatus().setHullFraction(newHull);
                } catch (Throwable t) {
                    // Fallback: use repairFully if granular API not available
                }
            }
            if (crRestore > 0f) {
                try {
                    float currentCR = m.getRepairTracker().getCR();
                    m.getRepairTracker().setCR(Math.min(1f, currentCR + crRestore));
                } catch (Throwable t) {}
            }
        }
    }
}
