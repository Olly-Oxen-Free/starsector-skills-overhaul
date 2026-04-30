package skills_overhaul.collective;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public final class VoltaicArcEmitter {

    private static final float ARC_RANGE       = 400f;  // [TUNE]
    private static final float ARC_EMP_PER_SEC = 25f;   // [TUNE]
    private static final float ARC_ENERGY_FRAC = 0.2f;

    /**
     * Emits PD-priority EMP arcs from source to nearest enemy within range.
     * Priority: missiles > fighters > ships.
     */
    public static void emitConduitsArcs(CombatEngineAPI engine, ShipAPI source, float amount) {
        List<CombatEntityAPI> missiles = new ArrayList<CombatEntityAPI>();
        List<CombatEntityAPI> fighters = new ArrayList<CombatEntityAPI>();
        List<CombatEntityAPI> ships    = new ArrayList<CombatEntityAPI>();

        for (ShipAPI target : engine.getShips()) {
            if (target.getOwner() == source.getOwner() || !target.isAlive()) continue;
            if (Misc.getDistance(source.getLocation(), target.getLocation()) > ARC_RANGE) continue;
            if (target.isFighter()) fighters.add(target);
            else ships.add(target);
        }
        for (MissileAPI m : engine.getMissiles()) {
            if (m.getOwner() == source.getOwner()) continue;
            if (Misc.getDistance(source.getLocation(), m.getLocation()) > ARC_RANGE) continue;
            missiles.add(m);
        }

        List<CombatEntityAPI> prioritized = new ArrayList<CombatEntityAPI>();
        prioritized.addAll(missiles);
        prioritized.addAll(fighters);
        prioritized.addAll(ships);
        if (prioritized.isEmpty()) return;

        CombatEntityAPI arcTarget = prioritized.get(0);
        float minDist = Float.MAX_VALUE;
        for (CombatEntityAPI e : prioritized) {
            float d = Misc.getDistance(source.getLocation(), e.getLocation());
            if (d < minDist) { minDist = d; arcTarget = e; }
        }

        float empDmg    = ARC_EMP_PER_SEC * amount;
        float energyDmg = empDmg * ARC_ENERGY_FRAC;
        engine.spawnEmpArc(source, source.getLocation(), source, arcTarget,
                DamageType.ENERGY, energyDmg, empDmg,
                ARC_RANGE, null, 0f, null, null);
    }

    /** Single chain arc for Voltaic Chain (T4). */
    public static void emitChainArc(CombatEngineAPI engine, ShipAPI source,
                                    CombatEntityAPI target, float empDmg, float energyDmg) {
        engine.spawnEmpArcPierceShields(source, source.getLocation(), source, target,
                DamageType.ENERGY, energyDmg, empDmg, 600f, null, 0f, null, null);
    }

    private VoltaicArcEmitter() {}
}
