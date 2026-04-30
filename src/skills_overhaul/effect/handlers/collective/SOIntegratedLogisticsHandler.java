package skills_overhaul.effect.handlers.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import org.json.JSONObject;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.effect.EffectHandler;

public final class SOIntegratedLogisticsHandler implements EffectHandler {

    private static final String SRC = "so_collective_integrated_logistics";

    // [TUNE] campaign stat bonuses
    private static final float FUEL_USE_MULT = 0.90f; // -10% fuel use

    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        MutableFleetStatsAPI stats = fleet.getStats();
        stats.getFuelUseHyperMult().modifyMult(SRC, FUEL_USE_MULT);
        stats.getFuelUseNormalMult().modifyMult(SRC, FUEL_USE_MULT);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        MutableFleetStatsAPI stats = fleet.getStats();
        stats.getFuelUseHyperMult().unmodify(SRC);
        stats.getFuelUseNormalMult().unmodify(SRC);
    }
}
