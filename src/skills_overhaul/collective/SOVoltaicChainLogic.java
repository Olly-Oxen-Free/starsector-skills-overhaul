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
        if (!(param instanceof WeaponAPI)) return null;

        WeaponAPI weapon = (WeaponAPI) param;
        if (weapon.isBeam()) return null;
        if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) return null;

        ShipAPI source = weapon.getShip();
        if (source == null || !isAICored(source)) return null;

        String pairKey = source.getId() + ":" + target.getId();
        tracker.addStack(pairKey);

        if (tracker.checkAndConsumeDetonation(pairKey)) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                VoltaicArcEmitter.emitChainArc(engine, source, target, EMP_DMG, ENERGY_DMG);
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
