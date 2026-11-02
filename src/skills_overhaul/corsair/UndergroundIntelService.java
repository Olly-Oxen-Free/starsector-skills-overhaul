package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public final class UndergroundIntelService {

    public static final String K_COOLDOWN_END = "$skills.corsair.intel.cooldownEnd";
    public static final float SEARCH_LY = 10f;
    public static final int MIN_FLEET_FP = 30;

    public enum Strength { WEAK, EVEN, STRONG }

    public static final class MarkResult {
        public CampaignFleetAPI fleet;
        public Strength strength;
        public String incentiveDescription;
        public boolean wasSpawned;
    }

    /** score = cargo + bounty + officer*5000 − abs(fleetFP − playerFP) * 0.5 */
    public static float score(float cargoValue, float bountyValue, int officerCount,
                              float fleetFP, float playerFP) {
        return cargoValue + bountyValue + officerCount * 5000f
                - Math.abs(fleetFP - playerFP) * 0.5f;
    }

    public static boolean inStrengthBand(float fp, float playerFP, Strength s) {
        float ratio = playerFP <= 0f ? 1f : fp / playerFP;
        switch (s) {
            case WEAK:   return ratio >= 0.60f && ratio <= 0.85f;
            case EVEN:   return ratio >= 0.85f && ratio <= 1.15f;
            case STRONG: return ratio >= 1.15f && ratio <= 1.40f;
            default:     return false;
        }
    }

    public static Strength rollStrength(Random rng) {
        float r = rng.nextFloat();
        if (r < 0.5f) return Strength.EVEN;
        if (r < 0.8f) return Strength.WEAK;
        return Strength.STRONG;
    }

    // --- Map-based overloads (unit-testable without Starsector runtime) ---

    public static boolean cooldownActive(Map<String, Object> mem, long now) {
        Object v = mem.get(K_COOLDOWN_END);
        if (!(v instanceof Number)) return false;
        return ((Number) v).longValue() > now;
    }

    public static void setCooldown(Map<String, Object> mem, long until) {
        mem.put(K_COOLDOWN_END, until);
    }

    // --- MemoryAPI overloads (live game) ---

    public static boolean cooldownActive(MemoryAPI mem, long now) {
        Object v = mem.get(K_COOLDOWN_END);
        if (!(v instanceof Number)) return false;
        return ((Number) v).longValue() > now;
    }

    public static void setCooldown(MemoryAPI mem, long until) {
        mem.set(K_COOLDOWN_END, until);
    }

    /**
     * Live: scans nearby fleets for the highest-scoring Mark in the rolled strength band.
     * Falls back to spawning a fresh fleet via FleetFactoryV3 if no candidate found.
     */
    public static MarkResult generateMark(Random rng) {
        MarkResult out = new MarkResult();
        out.strength = rollStrength(rng);

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        float playerFP = playerFleet.getFleetPoints();

        List<CampaignFleetAPI> nearby = Misc.getNearbyFleets(
                playerFleet, SEARCH_LY * Misc.getUnitsPerLightYear());

        CampaignFleetAPI best = null;
        float bestScore = -Float.MAX_VALUE;
        for (CampaignFleetAPI f : nearby) {
            if (f == playerFleet) continue;
            FactionAPI faction = f.getFaction();
            if (faction == null) continue;
            if (!faction.isHostileTo(Global.getSector().getPlayerFaction())) continue;
            float fp = f.getFleetPoints();
            if (fp < MIN_FLEET_FP) continue;
            if (!inStrengthBand(fp, playerFP, out.strength)) continue;

            float cargoValue = estimateCargoValue(f);
            float bountyValue = 0f;  // hook for bounty intel; v1 leaves at 0
            int officers = 0;
            if (f.getFleetData() != null && f.getFleetData().getOfficersCopy() != null) {
                officers = f.getFleetData().getOfficersCopy().size();
            }
            float s = score(cargoValue, bountyValue, officers, fp, playerFP);
            if (s > bestScore) { bestScore = s; best = f; }
        }

        if (best != null) {
            out.fleet = best;
            out.wasSpawned = false;
            out.incentiveDescription = describe(best);
            return out;
        }

        out.fleet = spawnFallback(out.strength, playerFP, rng);
        out.wasSpawned = true;
        out.incentiveDescription = out.fleet == null ? "no contacts available"
                : "fresh contact rallied via Underground (" + out.fleet.getFaction().getDisplayName() + ")";
        return out;
    }

    /** Estimates cargo value by summing stack quantity * base value per unit. */
    private static float estimateCargoValue(CampaignFleetAPI f) {
        if (f.getCargo() == null) return 0f;
        List<CargoStackAPI> stacks = f.getCargo().getStacksCopy();
        if (stacks == null) return 0f;
        float total = 0f;
        for (CargoStackAPI stack : stacks) {
            total += stack.getSize() * stack.getBaseValuePerUnit();
        }
        return total;
    }

    private static String describe(CampaignFleetAPI f) {
        StringBuilder sb = new StringBuilder();
        sb.append(f.getFaction().getDisplayName()).append(" — ");
        sb.append((int) f.getFleetPoints()).append(" FP");
        float credits = estimateCargoValue(f);
        if (credits > 0) sb.append("; cargo ~").append((int) credits).append(" cr");
        int officers = f.getFleetData() == null ? 0
                : (f.getFleetData().getOfficersCopy() == null ? 0
                  : f.getFleetData().getOfficersCopy().size());
        if (officers > 0) sb.append("; ").append(officers).append(" officers");
        return sb.toString();
    }

    private static CampaignFleetAPI spawnFallback(Strength s, float playerFP, Random rng) {
        float ratioMid = s == Strength.WEAK ? 0.725f : s == Strength.STRONG ? 1.275f : 1.0f;
        float fp = playerFP * ratioMid;

        // Only a hostile-owned market within SEARCH_LY can anchor the spawn (the tooltip promises
        // a mark within 10 LY, and MarkIntel drops marks kept beyond 30 LY). Hidden factions like
        // derelict/omega own no market and so never qualify.
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        List<MarketAPI> candidates = new ArrayList<>();
        List<Float> distancesLY = new ArrayList<>();
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
            FactionAPI fac = m.getFaction();
            if (fac == null || m.getPrimaryEntity() == null) continue;
            if (fac == playerFaction || !fac.isHostileTo(playerFaction)) continue;
            candidates.add(m);
            distancesLY.add(Misc.getDistanceLY(playerFleet, m.getPrimaryEntity()));
        }
        // None in range: report "no contacts" (the caller refunds) rather than a mark the
        // player cannot reach before it expires.
        int pick = MarkAnchorPicker.pick(distancesLY, SEARCH_LY, rng);
        if (pick < 0) return null;
        FactionAPI faction = candidates.get(pick).getFaction();
        SectorEntityToken anchor = candidates.get(pick).getPrimaryEntity();

        Vector2f locInHyper = anchor.getLocationInHyperspace();
        FleetParamsV3 params = new FleetParamsV3(
                locInHyper,
                faction.getId(),
                1f,
                "patrolFleet",
                fp,
                0f, 0f, 0f, 0f, 0f, 0f);
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null) return null;
        anchor.getContainingLocation().addEntity(fleet);
        fleet.setLocation(anchor.getLocation().x, anchor.getLocation().y);
        // Without assignments the spawned fleet idled at the market forever (fleet leak).
        fleet.addAssignment(com.fs.starfarer.api.campaign.FleetAssignment.PATROL_SYSTEM, anchor, 30f);
        fleet.addAssignment(com.fs.starfarer.api.campaign.FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, anchor, 1000f);
        return fleet;
    }

    private UndergroundIntelService() {}
}
