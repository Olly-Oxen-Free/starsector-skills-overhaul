package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Distributed Consciousness: when an AI-cored player ship is destroyed, distribute
 * flux dissipation + capacity bonuses to all surviving AI-cored player ships.
 *
 * Bonus per death event scales with the fallen ship's AI core tier:
 *   Alpha → +6%, Beta → +4%, Gamma/other → +2%
 * Capped at DEATH_CAP death events.
 *
 * Uses polling (wasAlive map) — no ShipDestroyedListener in this API version.
 */
public final class SODistributedConsciousnessListener {

    private static final int    DEATH_CAP    = 5;
    private static final String ALPHA_CORE_ID = "alpha_core";
    private static final String BETA_CORE_ID  = "beta_core";

    /** Tracks alive state of AI-cored player ships: shipId -> wasAlive. */
    private final Map<String, Boolean> wasAlive = new HashMap<String, Boolean>();

    /** Number of death events recorded so far (capped at DEATH_CAP). */
    private int deathCount = 0;

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_transspatial_mind")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0) continue;
            if (!isAICored(ship)) continue;

            String id = ship.getId();
            boolean prev    = wasAlive.containsKey(id) ? wasAlive.get(id) : true;
            boolean current = ship.isAlive();
            wasAlive.put(id, current);

            // Alive → dead transition
            if (prev && !current) {
                if (deathCount < DEATH_CAP) {
                    deathCount++;
                    float bonusPct = coreBonusPct(ship);
                    String source  = "so_distributed_" + deathCount;
                    applyToSurvivors(engine, source, bonusPct);
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyToSurvivors(CombatEngineAPI engine, String source, float bonusPct) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getFluxDissipation().modifyPercent(source, bonusPct);
            stats.getFluxCapacity().modifyPercent(source, bonusPct);
        }
    }

    private static float coreBonusPct(ShipAPI ship) {
        if (ship.getCaptain() == null) return 2f;
        String coreId = ship.getCaptain().getId();
        if (ALPHA_CORE_ID.equals(coreId)) return 6f;
        if (BETA_CORE_ID.equals(coreId))  return 4f;
        return 2f;
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
