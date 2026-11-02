package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

public final class SOFirstStrikeLogic {

    private static final String SRC        = "so_first_strike";
    private static final float  RANGE      = 1500f;
    private static final float  SPEED_BUFF = 30f;

    private final SOFirstStrikeTracker tracker = new SOFirstStrikeTracker();

    public void advance(CombatEngineAPI engine, float amount) {
        if (!CollectiveActiveNodes.isActive("so_collective_first_strike")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAutomated(ship)) continue;

            String id = ship.getId();
            SOFirstStrikeTracker.FSState state = tracker.getState(id);
            MutableShipStatsAPI stats = ship.getMutableStats();

            if (state == SOFirstStrikeTracker.FSState.UNENGAGED) {
                if (hasEnemyNearby(engine, ship, RANGE)) {
                    tracker.notifyContact(id);
                    clearSpeedBuff(stats);
                } else {
                    applySpeedBuff(stats);
                }
            } else {
                tracker.advance(id, amount);
                clearSpeedBuff(stats);
                float dmgPct = tracker.getDamageMultiplier(id) * 100f;
                if (dmgPct <= 0f) {
                    stats.getBallisticWeaponDamageMult().unmodify(SRC);
                    stats.getEnergyWeaponDamageMult().unmodify(SRC);
                    stats.getMissileWeaponDamageMult().unmodify(SRC);
                } else {
                    stats.getBallisticWeaponDamageMult().modifyPercent(SRC, dmgPct);
                    stats.getEnergyWeaponDamageMult().modifyPercent(SRC, dmgPct);
                    stats.getMissileWeaponDamageMult().modifyPercent(SRC, dmgPct);
                }
            }
        }
    }

    private void applySpeedBuff(MutableShipStatsAPI stats) {
        stats.getMaxSpeed().modifyPercent(SRC, SPEED_BUFF);
        stats.getTurnAcceleration().modifyPercent(SRC, SPEED_BUFF);
        stats.getMaxTurnRate().modifyPercent(SRC, SPEED_BUFF);
    }

    private void clearSpeedBuff(MutableShipStatsAPI stats) {
        stats.getMaxSpeed().unmodify(SRC);
        stats.getTurnAcceleration().unmodify(SRC);
        stats.getMaxTurnRate().unmodify(SRC);
    }

    private boolean hasEnemyNearby(CombatEngineAPI engine, ShipAPI ship, float range) {
        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() == ship.getOwner()) continue;
            if (!other.isAlive()) continue;
            if (Misc.getDistance(ship.getLocation(), other.getLocation()) <= range) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }

    private static boolean isAutomated(ShipAPI ship) {
        // Coreless automated hulls have a default (non-null) captain — check the hull itself.
        return isAICored(ship) || ship.getCaptain() == null || ship.isDrone()
                || com.fs.starfarer.api.util.Misc.isAutomated(ship);
    }
}
