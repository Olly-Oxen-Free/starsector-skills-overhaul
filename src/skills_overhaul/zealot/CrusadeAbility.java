package skills_overhaul.zealot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Map;

public final class CrusadeAbility extends BaseDurationAbility {

    public static final String ABILITY_ID = "skills_overhaul_crusade";

    @Override
    protected void activateImpl() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        long now = Global.getSector().getClock().getTimestamp();
        Map<String, Object> wrapper = new ZealDailyTicker.MemoryWrapMap(mem);

        if (!CrusadeService.isActive(wrapper)) {
            // Declare crusade — defense in depth: honor the 180-day cooldown even if isUsable() was bypassed.
            if (!CrusadeService.canDeclare(wrapper, now)) { this.deactivate(); return; }
            String rivalId = RivalryService.getCurrentRival(mem);
            if (rivalId == null) { this.deactivate(); return; }
            FactionAPI player = Global.getSector().getPlayerFaction();
            FactionAPI rival = Global.getSector().getFaction(rivalId);
            if (rival == null) { this.deactivate(); return; }

            // -30 rep with Rival and its allies
            player.adjustRelationship(rival.getId(), -0.30f);
            for (FactionAPI other : Global.getSector().getAllFactions()) {
                if (other == player || other == rival) continue;
                if (other.getRelationship(rival.getId()) > 0f) {
                    player.adjustRelationship(other.getId(), -0.30f);
                }
            }
            CrusadeService.declare(wrapper, rivalId, now);
            ZealService.registerCapBonus(wrapper, "crusade", CrusadeService.CAP_BONUS);
            CrusadeNPCStub.onPlayerCrusadeDeclared(rivalId);
        } else if (CrusadeService.canManualCancel(wrapper, now)) {
            // Cancel crusade
            ZealService.unregisterCapBonus(wrapper, "crusade");
            CrusadeService.manualCancel(wrapper, now);
        }
        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (RivalryService.getCurrentRival(mem) == null) return false;
        long now = Global.getSector().getClock().getTimestamp();
        Map<String, Object> wrapper = new ZealDailyTicker.MemoryWrapMap(mem);
        return CrusadeService.canDeclare(wrapper, now) || CrusadeService.canManualCancel(wrapper, now);
    }

    @Override
    public void createTooltip(TooltipMakerAPI t, boolean expanded) {
        t.addTitle("Crusade");
        t.addPara("Declare formal war on your Rival. -30 rep with Rival and allies. 6-month commitment.", 5f);
        t.addPara("Active: Zeal cap +50, post-battle hull restore +15% vs Rival, -10% damage taken from Rival.", 5f);
    }
}
