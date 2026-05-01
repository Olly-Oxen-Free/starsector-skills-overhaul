package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public final class SODissonanceProjector extends SOSecondarySystem {
    @Override protected float baseCooldown() { return 30f; }
    @Override protected float activeDuration() { return 0f; }
    @Override public String getDisplayName() { return "Dissonance Projector"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        float maxFlux = ship.getFluxTracker().getMaxFlux();
        float fluxFrac = maxFlux > 0f ? ship.getFluxTracker().getCurrFlux() / maxFlux : 0f;
        float scale = 0.5f + fluxFrac;
        float energyDmg = 300f * scale;
        float empDmg = 1200f * scale;

        DamagingExplosionSpec spec = new DamagingExplosionSpec(
            0.5f, 500f, 700f, energyDmg, energyDmg * 0.5f,
            CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER,
            4f, 16f, 0.8f, 30,
            new Color(100, 100, 255, 200), new Color(50, 50, 200, 150)
        );
        spec.setDamageType(DamageType.ENERGY);
        spec.setMaxEMPDamage(empDmg);
        spec.setMinEMPDamage(empDmg * 0.5f);
        engine.spawnDamagingExplosion(spec, ship, new Vector2f(ship.getLocation()), false);
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        tickCooldown(dt);
    }
}
