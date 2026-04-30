package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Mind Cascade: when an AI-cored ship is destroyed, mirror bonuses to the next
 * uncaptained automated ship on the player's side.
 *
 * Implemented as a per-frame advance check (no ShipDestroyedListener in this API version).
 * Tracks alive AI-cored ship IDs; on death transition, applies mods to recipient.
 */
public final class SOMindCascadeListener {

    private static final String SRC = "so_mind_cascade";

    /** Tracks alive state of AI-cored ships: id -> wasAlive. */
    private final Map<String, Boolean> aliveState = new HashMap<String, Boolean>();

    /** ID of the last ship that received cascade mods, or null if none. */
    private String lastRecipientId = null;

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_mind_cascade")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0) continue;
            if (!isAICored(ship)) continue;

            String id = ship.getId();
            boolean wasAlive = aliveState.containsKey(id) ? aliveState.get(id) : true;
            boolean isAlive  = ship.isAlive();
            aliveState.put(id, isAlive);

            // Transition: alive → dead
            if (wasAlive && !isAlive) {
                applyToRecipient(engine);
            }
        }
    }

    private void applyToRecipient(CombatEngineAPI engine) {
        // Remove mods from the previous recipient before applying to a new one
        if (lastRecipientId != null) {
            for (ShipAPI ship : engine.getShips()) {
                if (ship.getId().equals(lastRecipientId)) {
                    MutableShipStatsAPI prev = ship.getMutableStats();
                    prev.getHullDamageTakenMult().unmodify(SRC);
                    prev.getBallisticWeaponDamageMult().unmodify(SRC);
                    prev.getEnergyWeaponDamageMult().unmodify(SRC);
                    prev.getMissileWeaponDamageMult().unmodify(SRC);
                    prev.getEmpDamageTakenMult().unmodify(SRC);
                    break;
                }
            }
            lastRecipientId = null;
        }

        ShipAPI recipient = null;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0) continue;
            if (!ship.isAlive()) continue;
            if (!isAutomated(ship)) continue;
            if (!isUncaptained(ship)) continue;
            recipient = ship;
            break;
        }
        if (recipient == null) return;

        MutableShipStatsAPI stats = recipient.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(SRC, 0.85f);
        stats.getBallisticWeaponDamageMult().modifyPercent(SRC, 10f);
        stats.getEnergyWeaponDamageMult().modifyPercent(SRC, 10f);
        stats.getMissileWeaponDamageMult().modifyPercent(SRC, 10f);
        stats.getEmpDamageTakenMult().modifyMult(SRC, 0.75f);
        lastRecipientId = recipient.getId();
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }

    private static boolean isAutomated(ShipAPI ship) {
        return isAICored(ship) || ship.getCaptain() == null || ship.isDrone();
    }

    private static boolean isUncaptained(ShipAPI ship) {
        return ship.getCaptain() == null || ship.getCaptain().isDefault();
    }
}
