package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
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
    /** Credits awarded per fleet point of enemy strength destroyed by the privateer. */
    public static final float  CREDITS_PER_FP = 200f;
    /** Hard cap on the privateer's per-battle payout to the player. */
    public static final float  MAX_BATTLE_PAYOUT = 20000f;

    public static float computeFleetFP(int scrap) { return scrap * FP_PER_SCRAP; }
    public static boolean isCommissionAllowed(int scrap) { return scrap >= MIN_SCRAP; }
    public static float computeIncomeShare(float salvageValue) { return salvageValue * INCOME_SHARE; }

    /**
     * Per-battle payout to the player: the {@link #INCOME_SHARE} cut of the credit
     * value of the enemy fleet strength (in fleet points) the privateer destroyed,
     * capped at {@link #MAX_BATTLE_PAYOUT}.
     */
    public static float computePrivateerPayout(float destroyedFleetPoints) {
        if (destroyedFleetPoints <= 0f) return 0f;
        float raw = destroyedFleetPoints * CREDITS_PER_FP * INCOME_SHARE;
        return Math.min(raw, MAX_BATTLE_PAYOUT);
    }

    public static boolean isCommissioned(MemoryAPI mem) {
        Object id = mem.get(K_FLEET_ID);
        if (id == null) return false;
        CampaignFleetAPI privateer = findPrivateerFleet((String) id);
        return privateer != null && privateer.isAlive();
    }

    /**
     * Resolves the commissioned privateer fleet by its stored id across the whole
     * sector, not just the player's current location. Prefer a direct entity
     * lookup; fall back to scanning every location so a fleet that has drifted
     * out of the player's system is still found (otherwise the player could
     * commission a duplicate and orphan the old one).
     */
    static CampaignFleetAPI findPrivateerFleet(String id) {
        if (id == null) return null;
        SectorEntityToken entity = Global.getSector().getEntityById(id);
        if (entity instanceof CampaignFleetAPI) return (CampaignFleetAPI) entity;
        for (LocationAPI loc : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI f : loc.getFleets()) {
                if (id.equals(f.getId())) return f;
            }
        }
        return null;
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

        // Guard: the faction only exists if registered in factions.csv; setFaction on an unknown id
        // would fail, so keep the commission faction in that case.
        FactionAPI privFac = Global.getSector().getFaction("skills_overhaul_privateer");
        if (privFac != null) {
            privateer.setFaction("skills_overhaul_privateer", true);
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
            CampaignFleetAPI privateer = findPrivateerFleet((String) id);
            if (privateer != null && privateer.getContainingLocation() != null) {
                // despawn() fires reportFleetDespawned for listeners; removeEntity() skipped that.
                privateer.despawn(com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason.OTHER, null);
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
