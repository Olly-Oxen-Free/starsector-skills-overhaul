package skills_overhaul.commandant;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.supply.SupplyBufferService;

public final class CraftFuelAbility extends BaseDurationAbility {
    public static final String ABILITY_ID = "skills_overhaul_craft_fuel";
    public static final float COST = 20f;
    public static final float OUTPUT = 1f;
    public static final float COOLDOWN_DAYS = 2f;
    public static final String K_CD_END = "$skills.commandant.craft_fuel_cd_end";

    @Override
    protected void activateImpl() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        long now = Global.getSector().getClock().getTimestamp();
        Object cdEnd = mem.get(K_CD_END);
        if (cdEnd instanceof Number && ((Number) cdEnd).longValue() > now) { this.deactivate(); return; }

        SupplyBufferService.DrainResult dr =
                SupplyBufferService.tryConsume(mem, COST, SupplyBufferService.DrainMode.ABILITY);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = fleet.getCargo();
        float fleetSupplies = cargo.getCommodityQuantity("supplies");
        if (dr.fromFleet > fleetSupplies) {
            float curBuf = SkillMemory.getSupplyBuffer(mem);
            SkillMemory.setSupplyBuffer(mem, curBuf + dr.fromBuffer);
            this.deactivate();
            return;
        }
        if (dr.fromFleet > 0f) cargo.removeSupplies(dr.fromFleet);

        cargo.addCommodity("fuel", OUTPUT);
        long cdEndNew = now + (long)(COOLDOWN_DAYS * Global.getSector().getClock().getSecondsPerDay() * 1000L);
        mem.set(K_CD_END, cdEndNew);
        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        long now = Global.getSector().getClock().getTimestamp();
        Object cdEnd = Global.getSector().getMemoryWithoutUpdate().get(K_CD_END);
        return !(cdEnd instanceof Number) || ((Number) cdEnd).longValue() <= now;
    }

    @Override
    public void createTooltip(TooltipMakerAPI t, boolean expanded) {
        t.addTitle("Craft Fuel");
        t.addPara("Convert %s buffer/fleet supplies into %s fuel. CD: %s days.", 5f,
                Misc.getHighlightColor(),
                String.valueOf((int) COST), String.valueOf((int) OUTPUT),
                String.valueOf((int) COOLDOWN_DAYS));
    }
}
