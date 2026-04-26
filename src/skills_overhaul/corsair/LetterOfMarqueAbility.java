package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.state.SkillMemory;

public final class LetterOfMarqueAbility extends BaseDurationAbility {

    public static final String ABILITY_ID = "skills_overhaul_letter_of_marque";

    @Override
    protected void activateImpl() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        float buffer = SkillMemory.getSupplyBuffer(mem);
        if (buffer < PrivateerFleetService.MIN_SCRAP) {
            this.deactivate();
            return;
        }
        SkillMemory.setSupplyBuffer(mem, 0f);
        PrivateerFleetService.commission((int) buffer);
        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        return SkillMemory.getSupplyBuffer(mem) >= PrivateerFleetService.MIN_SCRAP
                && !PrivateerFleetService.isCommissioned(mem);
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle("Letter of Marque");
        tooltip.addPara("Spend supply buffer to commission a privateer fleet for 30 days.", 5f);
        tooltip.addPara("Min: %s buffer supplies. Fleet FP = buffer × 0.5.", 3f,
                Misc.getHighlightColor(),
                String.valueOf(PrivateerFleetService.MIN_SCRAP));
    }
}
