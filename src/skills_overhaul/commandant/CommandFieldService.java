package skills_overhaul.commandant;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandFieldService {

    public static final float BASE_RANGE = 1500f;
    public static final float BASE_PER_LEVEL = 0.01f;
    public static final float BASE_PER_OFFICER_CAP = 0.06f;
    public static final float BASE_FLEET_CAP = 0.12f;
    public static final float DEFAULT_AI_CORE_FACTOR = 0.75f;

    private static final Map<String, Float> PARAMS = new LinkedHashMap<>();

    public static void registerParam(String key, String param, float value) {
        PARAMS.put(key + ":" + param, value);
    }

    public static void unregisterParam(String key) {
        Iterator<String> it = PARAMS.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.startsWith(key + ":")) it.remove();
        }
    }

    public static float sumParam(String param) {
        float total = 0f;
        for (Map.Entry<String, Float> e : PARAMS.entrySet()) {
            if (e.getKey().endsWith(":" + param)) total += e.getValue();
        }
        return total;
    }

    public static float productParam(String param, float defaultMult) {
        float total = defaultMult;
        for (Map.Entry<String, Float> e : PARAMS.entrySet()) {
            if (e.getKey().endsWith(":" + param)) total *= e.getValue();
        }
        return total;
    }

    public static float computeEffectiveRange(int hullSizeOrdinal) {
        float range = BASE_RANGE + sumParam("rangeBumpFlat");
        String className = hullSizeName(hullSizeOrdinal);
        float classMult = 1f;
        for (Map.Entry<String, Float> e : PARAMS.entrySet()) {
            if (e.getKey().endsWith(":rangeBumpMultByClass:" + className)) classMult *= e.getValue();
        }
        return range * classMult;
    }

    private static String hullSizeName(int ordinal) {
        switch (ordinal) {
            case 1: return "FRIGATE";
            case 2: return "DESTROYER";
            case 3: return "CRUISER";
            case 4: return "CAPITAL_SHIP";
            default: return "FIGHTER";
        }
    }

    public static float[] computeTotals(int[] officerLevels, boolean[] aiCoreFlags,
                                        float perLevelScalingMultProduct, float fleetCapBonus) {
        float cap = BASE_FLEET_CAP + fleetCapBonus;
        float capRatio = cap / BASE_FLEET_CAP;
        float total = 0f;
        for (int i = 0; i < officerLevels.length; i++) {
            int lvl = officerLevels[i];
            if (lvl <= 0) continue;
            float bonus = lvl * BASE_PER_LEVEL * perLevelScalingMultProduct * capRatio;
            float perOfficerCap = BASE_PER_OFFICER_CAP * perLevelScalingMultProduct * capRatio;
            if (bonus > perOfficerCap) bonus = perOfficerCap;
            if (i < aiCoreFlags.length && aiCoreFlags[i]) bonus *= DEFAULT_AI_CORE_FACTOR;
            total += bonus;
        }
        if (total > cap) total = cap;
        return new float[]{total, total};
    }

    public static float[] computeTotalsForShip(ShipAPI ship) {
        if (ship == null) return new float[]{0f, 0f};
        int hullOrdinal = ship.getHullSize() == null ? 0 : ship.getHullSize().ordinal();
        float effRange = computeEffectiveRange(hullOrdinal);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return new float[]{0f, 0f};

        List<Integer> levels = new ArrayList<>();
        List<Boolean> aiCores = new ArrayList<>();
        for (ShipAPI o : engine.getShips()) {
            if (o.getOwner() != ship.getOwner()) continue;
            if (o == ship) continue;
            FleetMemberAPI fm = o.getFleetMember();
            if (fm == null) continue;
            PersonAPI captain = fm.getCaptain();
            if (captain == null || captain.isDefault()) continue;
            int lvl = captain.getStats().getLevel();
            if (lvl < 1) continue;
            float dist = Misc.getDistance(ship.getLocation(), o.getLocation());
            if (dist > effRange) continue;
            levels.add(lvl);
            aiCores.add(captain.isAICore());
        }

        int[] lvlArr = new int[levels.size()];
        boolean[] aiArr = new boolean[aiCores.size()];
        for (int i = 0; i < lvlArr.length; i++) {
            lvlArr[i] = levels.get(i);
            aiArr[i] = aiCores.get(i);
        }
        float scalingProduct = productParam("perLevelScalingMult", 1f);
        float capBonus = sumParam("fleetCapBonus");
        return computeTotals(lvlArr, aiArr, scalingProduct, capBonus);
    }

    // TODO Task 3: replace stub with full CommandFieldListener attach
    public static void attach(ShipAPI ship) {}

    private CommandFieldService() {}
}
