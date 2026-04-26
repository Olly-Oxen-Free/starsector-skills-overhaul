package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;

public final class PrivateerFleetService {

    public static final String K_FLEET_ID  = "$skills.corsair.privateer.fleetId";
    public static final String K_EXPIRE    = "$skills.corsair.privateer.expireDay";
    public static final int    DURATION_DAYS = 30;
    public static final int    MIN_SCRAP   = 50;
    public static final float  FP_PER_SCRAP = 0.5f;
    public static final float  INCOME_SHARE = 0.30f;

    public static float computeFleetFP(int scrap) { return scrap * FP_PER_SCRAP; }
    public static boolean isCommissionAllowed(int scrap) { return scrap >= MIN_SCRAP; }
    public static float computeIncomeShare(float salvageValue) { return salvageValue * INCOME_SHARE; }

    public static boolean isCommissioned(MemoryAPI mem) {
        Object id = mem.get(K_FLEET_ID);
        if (id == null) return false;
        SectorEntityToken playerLocation = Global.getSector().getPlayerFleet();
        if (playerLocation == null || playerLocation.getContainingLocation() == null) return false;
        for (CampaignFleetAPI f : playerLocation.getContainingLocation().getFleets()) {
            if (id.equals(f.getId())) return f.isAlive();
        }
        return false;
    }

    public static CampaignFleetAPI commission(int scrapSpent) {
        if (!isCommissionAllowed(scrapSpent)) return null;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (isCommissioned(mem)) return null;

        FactionAPI commissionFaction = pickCommissionFaction();
        float fp = computeFleetFP(scrapSpent);

        SectorEntityToken anchor = Global.getSector().getPlayerFleet();
        FleetParamsV3 params = new FleetParamsV3(
                anchor.getLocationInHyperspace(),
                commissionFaction.getId(),
                1f,
                "patrolFleet",
                fp,
                0f, 0f, 0f, 0f, 0f, 0f);
        CampaignFleetAPI privateer = FleetFactoryV3.createFleet(params);
        if (privateer == null) return null;

        privateer.setFaction("skills_overhaul_privateer", true);
        FactionAPI privFac = Global.getSector().getFaction("skills_overhaul_privateer");
        if (privFac != null) {
            privFac.setRelationship(Global.getSector().getPlayerFaction().getId(), 1.0f);
            for (FactionAPI f : Global.getSector().getAllFactions()) {
                if (f == Global.getSector().getPlayerFaction()) continue;
                if (f.isHostileTo(Global.getSector().getPlayerFaction())) {
                    privFac.setRelationship(f.getId(), -1.0f);
                }
            }
        }

        anchor.getContainingLocation().addEntity(privateer);
        privateer.setLocation(anchor.getLocation().x, anchor.getLocation().y);
        privateer.addScript(new PrivateerEscortAI(privateer));

        mem.set(K_FLEET_ID, privateer.getId());
        mem.set(K_EXPIRE, InfamyDailyTicker.monotonicDay() + DURATION_DAYS);
        return privateer;
    }

    public static void dismiss() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Object id = mem.get(K_FLEET_ID);
        if (id != null) {
            SectorEntityToken playerLocation = Global.getSector().getPlayerFleet();
            if (playerLocation != null && playerLocation.getContainingLocation() != null) {
                for (CampaignFleetAPI f : playerLocation.getContainingLocation().getFleets()) {
                    if (id.equals(f.getId())) {
                        f.getContainingLocation().removeEntity(f);
                        break;
                    }
                }
            }
        }
        mem.unset(K_FLEET_ID);
        mem.unset(K_EXPIRE);
    }

    private static FactionAPI pickCommissionFaction() {
        String commId = (String) Global.getSector().getMemoryWithoutUpdate()
                .get("$playerCommissionFaction");
        if (commId != null) {
            FactionAPI f = Global.getSector().getFaction(commId);
            if (f != null) return f;
        }
        FactionAPI pirates = Global.getSector().getFaction("pirates");
        return pirates != null ? pirates : Global.getSector().getPlayerFaction();
    }

    private PrivateerFleetService() {}
}
