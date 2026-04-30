package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public final class SOFurorMachinaeLogic implements DamageDealtModifier {

    private static final String SRC = "so_furor_machinae";

    private final SOFurorMachinaeTracker tracker = new SOFurorMachinaeTracker();

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (!CollectiveActiveNodes.isActive("so_collective_furor_machinae")) return null;
        if (!(param instanceof WeaponAPI)) return null;

        WeaponAPI weapon = (WeaponAPI) param;
        ShipAPI source = weapon.getShip();
        if (source == null || !isAICored(source)) return null;

        // Skip beams and missiles
        if (weapon.isBeam()) return null;
        if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) return null;

        float weight = shieldHit ? 0.5f : 1f;
        tracker.recordHit(source.getId(), weight);
        return null;
    }

    public void advance(CombatEngineAPI engine, float amount) {
        if (!CollectiveActiveNodes.isActive("so_collective_furor_machinae")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAICored(ship)) continue;

            String id = ship.getId();
            tracker.advance(id, amount);

            float stacks = tracker.getStacks(id);
            float bonus  = Math.min(stacks * 0.02f, 0.20f) * 100f;
            MutableShipStatsAPI stats = ship.getMutableStats();

            if (bonus <= 0f) {
                stats.getBallisticRoFMult().unmodify(SRC);
                stats.getEnergyRoFMult().unmodify(SRC);
            } else {
                stats.getBallisticRoFMult().modifyPercent(SRC, bonus);
                stats.getEnergyRoFMult().modifyPercent(SRC, bonus);
            }
        }
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
