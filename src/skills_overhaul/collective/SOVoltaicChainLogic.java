package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public final class SOVoltaicChainLogic implements DamageDealtModifier {

    private static final float EMP_DMG    = 1000f;
    private static final float ENERGY_DMG = 500f;

    private final ChargeStackTracker tracker = new ChargeStackTracker();

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (!CollectiveActiveNodes.isActive("so_collective_voltaic_chain")) return null;
        // param is the damaging object (projectile/beam/arc), never a WeaponAPI — see the
        // DamageDealtModifier javadoc. Missiles are projectiles too, so exclude them first.
        if (param instanceof com.fs.starfarer.api.combat.MissileAPI) return null;
        if (!(param instanceof com.fs.starfarer.api.combat.DamagingProjectileAPI)) return null;
        WeaponAPI weapon = ((com.fs.starfarer.api.combat.DamagingProjectileAPI) param).getWeapon();
        if (weapon == null) return null;
        if (weapon.isBeam()) return null;
        if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) return null;

        ShipAPI source = weapon.getShip();
        if (source == null || !isAICored(source)) return null;

        // Only apply aura to ships, not other entities
        if (!(target instanceof ShipAPI)) return null;
        ShipAPI targetShip = (ShipAPI) target;

        String pairKey = source.getId() + ":" + targetShip.getId();
        tracker.addStack(pairKey);

        if (tracker.checkAndConsumeDetonation(pairKey)) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                VoltaicArcEmitter.emitChainArc(engine, source, targetShip, EMP_DMG, ENERGY_DMG);
            }
        }

        return null;
    }

    public void advance(float amount) {
        tracker.advance(amount);
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
