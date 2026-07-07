package skills_overhaul.corsair;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class PrivateerLifecycleScript extends BaseCampaignEventListener
        implements EveryFrameScript {

    public PrivateerLifecycleScript() { super(false); }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Object expire = mem.get(PrivateerFleetService.K_EXPIRE);
        if (!(expire instanceof Number)) return;
        int expireDay = ((Number) expire).intValue();
        int today = InfamyDailyTicker.monotonicDay();
        if (today >= expireDay) {
            PrivateerFleetService.dismiss();
        }
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle == null || primaryWinner == null) return;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Object id = mem.get(PrivateerFleetService.K_FLEET_ID);
        if (id == null) return;
        if (battle.isPlayerInvolved()) return;

        boolean privateerWon = false;
        java.util.List<CampaignFleetAPI> winningSide = battle.getSnapshotSideFor(primaryWinner);
        if (winningSide == null) return;
        for (CampaignFleetAPI f : winningSide) {
            if (id.equals(f.getId())) { privateerWon = true; break; }
        }
        if (!privateerWon) return;

        // Pay the player a share of the enemy fleet strength the privateer actually
        // destroyed on the LOSING side (the side opposite the primary winner), based
        // on fleet points rather than looted cargo value, and capped per battle.
        float destroyedFP = 0f;
        java.util.List<CampaignFleetAPI> losers = battle.getOtherSideSnapshotFor(primaryWinner);
        if (losers != null) {
            for (CampaignFleetAPI loser : losers) {
                if (loser == null || loser.getFleetData() == null) continue;
                java.util.List<FleetMemberAPI> before = loser.getFleetData().getSnapshot();
                if (before == null) continue;
                java.util.List<FleetMemberAPI> after = loser.getFleetData().getMembersListCopy();
                for (FleetMemberAPI m : before) {
                    if (m == null) continue;
                    if (after == null || !after.contains(m)) {
                        destroyedFP += m.getDeploymentPointsCost();
                    }
                }
            }
        }
        float share = PrivateerFleetService.computePrivateerPayout(destroyedFP);
        if (share > 0f) {
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(share);
        }
    }
}
