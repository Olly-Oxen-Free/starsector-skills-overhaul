package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.supply.SupplyBufferService;

import java.util.Random;

public final class UndergroundIntelAbility extends BaseDurationAbility {

    public static final String ABILITY_ID = "skills_overhaul_underground_intel";

    public static final float COOLDOWN_DAYS = 5f;
    public static final float COST_BASE = 50f;
    public static final float COST_LEGEND = 25f;

    @Override
    protected void activateImpl() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        long now = Global.getSector().getClock().getTimestamp();

        if (UndergroundIntelService.cooldownActive(mem, now)) { this.deactivate(); return; }

        int today = InfamyDailyTicker.monotonicDay();
        boolean legend = InfamyService.tierActive(mem, today, InfamyTier.LEGEND);
        float cost = legend ? COST_LEGEND : COST_BASE;

        SupplyBufferService.DrainResult dr =
                SupplyBufferService.tryConsume(mem, cost, SupplyBufferService.DrainMode.ABILITY);
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet.getCargo();
        float fleetSupplies = cargo.getCommodityQuantity("supplies");
        if (dr.fromFleet > fleetSupplies) {
            // Refund buffer drain; abort.
            float bufferRefund = dr.fromBuffer;
            float currentBuffer = skills_overhaul.state.SkillMemory.getSupplyBuffer(mem);
            skills_overhaul.state.SkillMemory.setSupplyBuffer(mem, currentBuffer + bufferRefund);
            this.deactivate();
            return;
        }
        if (dr.fromFleet > 0f) cargo.removeSupplies(dr.fromFleet);

        UndergroundIntelService.MarkResult mark = UndergroundIntelService.generateMark(new Random());
        if (mark.fleet == null) {
            // Refund and bail.
            float currentBuffer = skills_overhaul.state.SkillMemory.getSupplyBuffer(mem);
            skills_overhaul.state.SkillMemory.setSupplyBuffer(mem, currentBuffer + dr.fromBuffer);
            cargo.addSupplies(dr.fromFleet);
            this.deactivate();
            return;
        }

        MarkIntel intel = new MarkIntel(mark.fleet, mark.strength, mark.incentiveDescription);
        Global.getSector().getIntelManager().addIntel(intel);

        // Clock timestamps are calendar ms: one game day = 86400 * 1000 (not getSecondsPerDay()).
        long cooldownEnd = now + (long) (COOLDOWN_DAYS * 86400L * 1000L);
        UndergroundIntelService.setCooldown(mem, cooldownEnd);

        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        return !UndergroundIntelService.cooldownActive(
                Global.getSector().getMemoryWithoutUpdate(),
                Global.getSector().getClock().getTimestamp());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle("Underground Intel");
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int today = InfamyDailyTicker.monotonicDay();
        boolean legend = InfamyService.tierActive(mem, today, InfamyTier.LEGEND);
        float cost = legend ? COST_LEGEND : COST_BASE;
        tooltip.addPara("Cost: %s supplies", 5f, Misc.getHighlightColor(),
                Misc.getRoundedValueMaxOneAfterDecimal(cost));
        tooltip.addPara("Cooldown: 5 days", 3f);
        tooltip.addPara("Queries the Underground for a profitable mark within 10 LY.", 5f);
    }
}
