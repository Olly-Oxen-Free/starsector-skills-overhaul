package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public final class MarkIntel extends BaseIntelPlugin {

    private final CampaignFleetAPI markedFleet;
    private final String incentive;
    private final UndergroundIntelService.Strength strength;
    private float daysOutOfRange = 0f;

    public MarkIntel(CampaignFleetAPI markedFleet, UndergroundIntelService.Strength strength,
                     String incentive) {
        this.markedFleet = markedFleet;
        this.strength = strength;
        this.incentive = incentive;
    }

    @Override
    protected void advanceImpl(float amount) {
        if (markedFleet == null || !markedFleet.isAlive()) {
            endAfterDelay();
            return;
        }
        float distLY = Misc.getDistanceLY(Global.getSector().getPlayerFleet(), markedFleet);
        if (distLY > 30f) {
            daysOutOfRange += Misc.getDays(amount);
            if (daysOutOfRange > 7f) endAfterDelay();
        } else {
            daysOutOfRange = 0f;
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return markedFleet != null && markedFleet.isAlive() ? markedFleet : null;
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "raid");
    }

    @Override
    public java.util.Set<String> getIntelTags(SectorMapAPI map) {
        java.util.Set<String> tags = super.getIntelTags(map);
        tags.add("Underground Intel");
        return tags;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        info.addPara("Underground Mark — %s", 0f, Misc.getHighlightColor(),
                strength.name().toLowerCase());
        info.addPara(incentive, 3f);
    }
}
