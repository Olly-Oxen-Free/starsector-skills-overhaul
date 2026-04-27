package skills_overhaul.artificer.alphastorm;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.artificer.alphastorm.AlphaStormState.Intensity;

import java.util.Random;

public final class AlphaStormLightning {

    private static final Random RNG = new Random();

    public static float probability(float fluxLevel, Intensity intensity) {
        if (intensity == null) return 0f;
        float intensityMult;
        switch (intensity) {
            case MILD:     intensityMult = 1f; break;
            case MODERATE: intensityMult = 1.5f; break;
            case SEVERE:   intensityMult = 2f; break;
            default: intensityMult = 1f;
        }
        if (fluxLevel < 0.5f) return 0.001f * intensityMult;
        if (fluxLevel < 0.9f) return 0.005f * intensityMult;
        return 0.015f * intensityMult;
    }

    public static int empDamage(Intensity i) {
        if (i == null) return 0;
        switch (i) {
            case MILD: return 100;
            case MODERATE: return 200;
            case SEVERE: return 300;
            default: return 0;
        }
    }

    public static int energyDamage(Intensity i) {
        if (i == null) return 0;
        switch (i) {
            case MILD: return 50;
            case MODERATE: return 100;
            case SEVERE: return 150;
            default: return 0;
        }
    }

    public static void rollAndStrike(CombatEngineAPI engine, ShipAPI s, Intensity intensity, float dt) {
        float flux = s.getFluxLevel();
        float p = probability(flux, intensity) * dt;
        if (RNG.nextFloat() > p) return;
        strike(engine, s, intensity);

        // Chain on overload
        if (s.getFluxTracker() != null && s.getFluxTracker().isOverloadedOrVenting()) {
            for (ShipAPI o : engine.getShips()) {
                if (o == s || !o.isAlive()) continue;
                if (Misc.getDistance(s.getLocation(), o.getLocation()) > 600f) continue;
                strike(engine, o, intensity);
            }
        }
    }

    private static void strike(CombatEngineAPI engine, ShipAPI target, Intensity i) {
        try {
            engine.applyDamage(target, target.getLocation(),
                    energyDamage(i), DamageType.ENERGY,
                    empDamage(i), false, false, null);
        } catch (Throwable t) {}
    }

    private AlphaStormLightning() {}
}
