package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CollectiveCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static final String SRC              = "so_collective_assimilation";
    private static final String SRC_FIRE         = "so_collective_precision_fire_control";
    private static final String SRC_MEM          = "so_collective_combat_memory";
    private static final String SRC_EMP          = "so_collective_magnetic_shielding";
    private static final String SRC_PERSIST      = "so_collective_deadly_persistence";
    private static final String SRC_CONDITIONING = "so_collective_battle_conditioning";
    private static final String SRC_BURST        = "so_collective_shield_burst";
    private static final String SRC_INTEGRITY    = "so_collective_structural_integrity";
    private static final String SRC_TARGETING    = "so_collective_hardened_targeting";

    // Shield Burst: track exposure timer and burst timer per ship (by id)
    private final Map<String, Float> exposedTimer = new HashMap<String, Float>();
    private final Map<String, Float> burstTimer   = new HashMap<String, Float>();
    private static final float BURST_EXPOSE_THRESHOLD = 2.5f; // [TUNE]
    private static final float BURST_DURATION         = 5.0f; // [TUNE]
    private static final float BURST_DMG_BONUS        = 0.30f; // [TUNE]

    // Structural Integrity cache
    private float integrityUpdateTimer = 0f;
    private static final float INTEGRITY_UPDATE_INTERVAL = 0.25f;

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
        // T2
        if (CollectiveActiveNodes.isActive("so_collective_deadly_persistence")) {
            advanceDeadlyPersistence(engine);
        }
        if (CollectiveActiveNodes.isActive("so_collective_battle_conditioning")) {
            advanceBattleConditioning(engine);
        }
        if (CollectiveActiveNodes.isActive("so_collective_shield_burst")) {
            advanceShieldBurst(engine, amount);
        }
        if (CollectiveActiveNodes.isActive("so_collective_structural_integrity")) {
            advanceStructuralIntegrity(engine, amount);
        }
        if (CollectiveActiveNodes.isActive("so_collective_hardened_targeting")) {
            advanceHardenedTargeting(engine);
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
        // Detect primary enemy faction (most ships on side 1)
        Map<String, Integer> factionCounts = new HashMap<String, Integer>();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 1 || !ship.isAlive()) continue;
            String faction = "unknown";
            try {
                if (ship.getFleetMember() != null
                        && ship.getFleetMember().getFleetData() != null
                        && ship.getFleetMember().getFleetData().getFleet() != null
                        && ship.getFleetMember().getFleetData().getFleet().getFaction() != null) {
                    faction = ship.getFleetMember().getFleetData().getFleet().getFaction().getId();
                }
            } catch (Exception ignored) {}
            int count = factionCounts.containsKey(faction) ? factionCounts.get(faction) : 0;
            factionCounts.put(faction, count + 1);
        }

        String primaryFaction = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : factionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                primaryFaction = entry.getKey();
            }
        }
        if (primaryFaction == null) return;

        memoryTracker.recordEngagement(primaryFaction);
        float dmgBonus    = memoryTracker.getDamageBonus(primaryFaction);
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

    // ── T2 ────────────────────────────────────────────────────────────────────

    private void advanceDeadlyPersistence(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            // ZFB threshold +5% (flat addition to minimum flux level for ZFB)
            stats.getZeroFluxMinimumFluxLevel().modifyFlat(SRC_PERSIST, 0.05f);
            // Vent rate +20%
            stats.getVentRateMult().modifyPercent(SRC_PERSIST, 20f);
        }
    }

    private void advanceBattleConditioning(CombatEngineAPI engine) {
        MemoryView view = new MemoryAPIView(Global.getSector().getMemoryWithoutUpdate());
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            String coreId = ship.getCaptain().getId();
            KnowledgeTier tier = KnowledgeTracker.getTier(view, coreId);

            float fluxCapBonus = tier.ordinalTier >= 1
                    ? Math.min(30f, 10f + (tier.ordinalTier - 1) * 10f) : 0f;
            float fluxDisBonus = tier.ordinalTier >= 2
                    ? Math.min(25f, 15f + (tier.ordinalTier - 2) * 5f) : 0f;
            float armorBonus   = tier.ordinalTier >= 3 ? 15f : 0f;

            MutableShipStatsAPI stats = ship.getMutableStats();
            if (fluxCapBonus > 0f)
                stats.getFluxCapacity().modifyPercent(SRC_CONDITIONING, fluxCapBonus);
            if (fluxDisBonus > 0f)
                stats.getFluxDissipation().modifyPercent(SRC_CONDITIONING, fluxDisBonus);
            if (armorBonus > 0f)
                stats.getArmorBonus().modifyFlat(SRC_CONDITIONING, armorBonus);
        }
    }

    private void advanceShieldBurst(CombatEngineAPI engine, float amount) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            String id = ship.getId();
            MutableShipStatsAPI stats = ship.getMutableStats();

            boolean shieldDown = ship.getShield() == null || !ship.getShield().isOn();
            float exposed = exposedTimer.containsKey(id) ? exposedTimer.get(id) : 0f;
            float burst   = burstTimer.containsKey(id)   ? burstTimer.get(id)   : 0f;

            if (burst > 0f) {
                burst -= amount;
                if (burst <= 0f) {
                    burst = 0f;
                    stats.getBallisticWeaponDamageMult().unmodify(SRC_BURST);
                    stats.getEnergyWeaponDamageMult().unmodify(SRC_BURST);
                    stats.getMissileWeaponDamageMult().unmodify(SRC_BURST);
                }
                burstTimer.put(id, burst);
                // reset exposure
                exposedTimer.put(id, 0f);
            } else if (shieldDown) {
                exposed += amount;
                if (exposed >= BURST_EXPOSE_THRESHOLD) {
                    // trigger burst
                    burst = BURST_DURATION;
                    exposed = 0f;
                    stats.getBallisticWeaponDamageMult().modifyMult(SRC_BURST, 1f + BURST_DMG_BONUS);
                    stats.getEnergyWeaponDamageMult().modifyMult(SRC_BURST, 1f + BURST_DMG_BONUS);
                    stats.getMissileWeaponDamageMult().modifyMult(SRC_BURST, 1f + BURST_DMG_BONUS);
                }
                exposedTimer.put(id, exposed);
                burstTimer.put(id, burst);
            } else {
                // shield is up, decay exposure
                exposedTimer.put(id, Math.max(0f, exposed - amount));
            }
        }
    }

    private void advanceStructuralIntegrity(CombatEngineAPI engine, float amount) {
        integrityUpdateTimer += amount;
        if (integrityUpdateTimer < INTEGRITY_UPDATE_INTERVAL) return;
        integrityUpdateTimer = 0f;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            float armorFraction = computeArmorFraction(ship);
            float hullFraction  = ship.getHullLevel();
            boolean critical    = hullFraction < 0.5f;

            float drFloor, drCeil;
            if (critical) {
                drFloor = 0.15f; drCeil = 0.30f;
            } else {
                drFloor = 0.08f; drCeil = 0.20f;
            }
            float dr = drFloor + (drCeil - drFloor) * armorFraction;

            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getHullDamageTakenMult().modifyMult(SRC_INTEGRITY, 1f - dr);
        }
    }

    private float computeArmorFraction(ShipAPI ship) {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null) return 1f;
        float[][] cells = grid.getGrid();
        if (cells == null || cells.length == 0) return 1f;
        float total = 0f;
        float max   = grid.getMaxArmorInCell();
        if (max <= 0f) return 1f;
        int count = 0;
        for (float[] row : cells) {
            for (float cell : row) {
                total += cell;
                count++;
            }
        }
        if (count == 0) return 1f;
        return (total / count) / max;
    }

    private void advanceHardenedTargeting(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getMaxRecoilMult().modifyMult(SRC_TARGETING, 0f);
            stats.getRecoilPerShotMult().modifyMult(SRC_TARGETING, 0f);
        }
    }
}
