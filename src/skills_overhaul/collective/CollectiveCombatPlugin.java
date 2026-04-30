package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public final class CollectiveCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static final String SRC = "so_collective_assimilation";
    private static final String SRC_FIRE   = "so_collective_precision_fire_control";
    private static final String SRC_MEM    = "so_collective_combat_memory";
    private static final String SRC_EMP    = "so_collective_magnetic_shielding";

    private final CombatMemoryTracker memoryTracker = new CombatMemoryTracker();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (!CollectiveActiveNodes.isActive("so_collective_t0")) return;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        applyAssimilationPassives(mem, engine);

        if (CollectiveActiveNodes.isActive("so_collective_precision_fire_control")) {
            advancePrecisionFireControl(engine);
        }
        if (CollectiveActiveNodes.isActive("so_collective_magnetic_shielding")) {
            advanceMagneticShielding(engine);
        }
        if (CollectiveActiveNodes.isActive("so_collective_combat_memory")) {
            advanceCombatMemory(engine);
        }
        if (CollectiveActiveNodes.isActive("so_collective_voltaic_conduits")) {
            advanceVoltaicConduits(engine, amount);
        }
    }

    public static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }

    private static boolean isAutomated(ShipAPI ship) {
        return isAICored(ship) || ship.getCaptain() == null || ship.isDrone();
    }

    private void applyAssimilationPassives(MemoryAPI mem, CombatEngineAPI engine) {
        MemoryView view = new MemoryAPIView(mem);
        float crBonus   = AssimilationRegistry.getCRBonus(view);
        float fluxBonus = AssimilationRegistry.getFluxDissipationBonus(view);
        float dmgBonus  = AssimilationRegistry.getWeaponDamageBonus(view);

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            if (crBonus > 0f)
                stats.getMaxCombatReadiness().modifyPercent(SRC, crBonus);
            if (fluxBonus > 0f)
                stats.getFluxDissipation().modifyPercent(SRC, fluxBonus);
            if (dmgBonus > 0f) {
                stats.getBallisticWeaponDamageMult().modifyPercent(SRC, dmgBonus);
                stats.getEnergyWeaponDamageMult().modifyPercent(SRC, dmgBonus);
                stats.getMissileWeaponDamageMult().modifyPercent(SRC, dmgBonus);
            }
        }
    }

    private void advancePrecisionFireControl(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getBallisticWeaponDamageMult().modifyMult(SRC_FIRE, 1.10f);
            stats.getEnergyWeaponDamageMult().modifyMult(SRC_FIRE, 1.10f);
        }
    }

    private void advanceMagneticShielding(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAutomated(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getEmpDamageTakenMult().modifyMult(SRC_EMP, 0.75f);
        }
    }

    private void advanceCombatMemory(CombatEngineAPI engine) {
        // Detect primary enemy faction (most ships)
        java.util.Map<String, Integer> factionCounts = new java.util.HashMap<String, Integer>();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == 0 || !ship.isAlive()) continue;
            String fid = ship.getHullSpec().getManufacturer(); // rough proxy; use owner faction
            if (ship.getOwner() == 1) {
                // enemy ship
                String faction = "unknown";
                if (ship.getFleetMember() != null && ship.getFleetMember().getFleetData() != null
                        && ship.getFleetMember().getFleetData().getFleet() != null
                        && ship.getFleetMember().getFleetData().getFleet().getFaction() != null) {
                    faction = ship.getFleetMember().getFleetData().getFleet().getFaction().getId();
                }
                int count = factionCounts.containsKey(faction) ? factionCounts.get(faction) : 0;
                factionCounts.put(faction, count + 1);
            }
        }

        String primaryFaction = null;
        int maxCount = 0;
        for (java.util.Map.Entry<String, Integer> entry : factionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                primaryFaction = entry.getKey();
            }
        }
        if (primaryFaction == null) return;

        memoryTracker.recordEngagement(primaryFaction);
        float dmgBonus   = memoryTracker.getDamageBonus(primaryFaction);
        float resistBonus = memoryTracker.getResistBonus(primaryFaction);

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            if (dmgBonus > 0f) {
                stats.getBallisticWeaponDamageMult().modifyMult(SRC_MEM, 1f + dmgBonus / 100f);
                stats.getEnergyWeaponDamageMult().modifyMult(SRC_MEM, 1f + dmgBonus / 100f);
                stats.getMissileWeaponDamageMult().modifyMult(SRC_MEM, 1f + dmgBonus / 100f);
            }
            if (resistBonus > 0f) {
                stats.getHullDamageTakenMult().modifyMult(SRC_MEM, 1f - resistBonus / 100f);
            }
        }
    }

    private void advanceVoltaicConduits(CombatEngineAPI engine, float amount) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            VoltaicArcEmitter.emitConduitsArcs(engine, ship, amount);
        }
    }
}
