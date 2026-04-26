package skills_overhaul.corsair;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

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

        float loot = 0f;
        java.util.List<CampaignFleetAPI> losers = battle.getNonPlayerSide();
        if (losers != null) {
            for (CampaignFleetAPI loser : losers) {
                if (loser == null || loser.getCargo() == null) continue;
                for (CargoStackAPI st : loser.getCargo().getStacksCopy()) {
                    loot += st.getSize() * st.getBaseValuePerUnit();
                }
            }
        }
        float share = PrivateerFleetService.computeIncomeShare(loot);
        if (share > 0f) {
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(share);
        }
    }
}
