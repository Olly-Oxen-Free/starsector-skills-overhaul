package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.supply.SupplyBufferService;

public final class MobileFabUnitTicker implements EveryFrameScript {

    public static final String K_LAST_BILLED = "$skills.artificer.forge.fab_last_billed_day";
    public static final float DAILY_COST = 1f;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (!Boolean.TRUE.equals(mem.get("$skills.artificer.t0.purchased"))) return;

        int today = monotonicDay();
        Object lastObj = mem.get(K_LAST_BILLED);
        Integer last = lastObj instanceof Number ? ((Number) lastObj).intValue() : null;
        if (last == null) {
            mem.set(K_LAST_BILLED, today);
            return;
        }
        if (today <= last) return;

        int days = Math.min(7, today - last);
        for (int i = 0; i < days; i++) {
            SupplyBufferService.DrainResult dr =
                    SupplyBufferService.tryConsume(mem, DAILY_COST,
                            SupplyBufferService.DrainMode.UPKEEP);
            CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
            float fleetSupplies = cargo.getCommodityQuantity("supplies");
            if (dr.fromFleet > fleetSupplies) {
                float curBuf = SkillMemory.getSupplyBuffer(mem);
                SkillMemory.setSupplyBuffer(mem, curBuf + dr.fromBuffer);
                mem.set(ForgeService.K_FAB_INACTIVE, true);
                break;
            }
            if (dr.fromFleet > 0f) cargo.removeCommodity("supplies", dr.fromFleet);
            mem.unset(ForgeService.K_FAB_INACTIVE);
        }
        mem.set(K_LAST_BILLED, today);
    }

    public static int monotonicDay() {
        return skills_overhaul.artificer.alphastorm.AlphaStormAbility.monotonicDay();
    }
}
