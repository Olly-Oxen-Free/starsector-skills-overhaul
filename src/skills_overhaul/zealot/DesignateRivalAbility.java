package skills_overhaul.zealot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public final class DesignateRivalAbility extends BaseDurationAbility {

    public static final String ABILITY_ID = "skills_overhaul_designate_rival";

    @Override
    protected void activateImpl() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        long now = Global.getSector().getClock().getTimestamp();
        if (!RivalryService.canDesignate(mem, now)) { this.deactivate(); return; }

        FactionAPI player = Global.getSector().getPlayerFaction();
        FactionAPI mostHostile = null;
        float worstRel = 0f;
        for (FactionAPI f : Global.getSector().getAllFactions()) {
            if (f == player) continue;
            // Hidden factions (remnant, derelict, omega, ...) are not valid rivals; the spec lists
            // "currently-hostile factions" as the player would see them.
            if (!f.isShowInIntelTab()) continue;
            float rel = player.getRelationship(f.getId());
            if (rel < worstRel) {
                worstRel = rel;
                mostHostile = f;
            }
        }
        if (mostHostile != null) {
            RivalryService.designate(mem, mostHostile.getId(), now);
        }
        this.deactivate();
    }

    @Override protected void applyEffect(float amount, float level) {}
    @Override protected void deactivateImpl() {}
    @Override protected void cleanupImpl() {}

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        long now = Global.getSector().getClock().getTimestamp();
        return RivalryService.canDesignate(Global.getSector().getMemoryWithoutUpdate(), now);
    }

    @Override
    public void createTooltip(TooltipMakerAPI t, boolean expanded) {
        t.addTitle("Designate Rival");
        t.addPara("Designate the most hostile faction as your Rival. 30-day cooldown to swap.", 5f);
    }
}
