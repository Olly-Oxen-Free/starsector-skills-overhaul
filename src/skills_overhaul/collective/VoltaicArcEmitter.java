package skills_overhaul.collective;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public final class VoltaicArcEmitter {

    private static final float ARC_RANGE       = 400f;  // [TUNE]
    private static final float ARC_EMP_PER_SEC = 25f;   // [TUNE]
    private static final float ARC_ENERGY_FRAC = 0.2f;
    // Explicit arc colours/thickness: null colours and 0 thickness gave an invisible (or unsafe) arc.
    private static final java.awt.Color ARC_FRINGE = new java.awt.Color(120, 180, 255);
    private static final java.awt.Color ARC_CORE   = java.awt.Color.WHITE;

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

        // Strict PD priority: nearest within highest non-empty class
        List<CombatEntityAPI> chosen = !missiles.isEmpty() ? missiles
                : !fighters.isEmpty() ? fighters : ships;
        if (chosen.isEmpty()) return;

        CombatEntityAPI arcTarget = chosen.get(0);
        float minDist = Float.MAX_VALUE;
        for (CombatEntityAPI e : chosen) {
            float d = Misc.getDistance(source.getLocation(), e.getLocation());
            if (d < minDist) { minDist = d; arcTarget = e; }
        }

        float empDmg    = ARC_EMP_PER_SEC * amount;
        float energyDmg = empDmg * ARC_ENERGY_FRAC;
        engine.spawnEmpArc(source, source.getLocation(), source, arcTarget,
                DamageType.ENERGY, energyDmg, empDmg,
                ARC_RANGE, null, 5f, ARC_FRINGE, ARC_CORE);
    }

    /** Single chain arc for Voltaic Chain (T4). */
    public static void emitChainArc(CombatEngineAPI engine, ShipAPI source,
                                    CombatEntityAPI target, float empDmg, float energyDmg) {
        engine.spawnEmpArcPierceShields(source, source.getLocation(), source, target,
                DamageType.ENERGY, energyDmg, empDmg, 600f, null, 10f, ARC_FRINGE, ARC_CORE);
    }

    private VoltaicArcEmitter() {}
}
