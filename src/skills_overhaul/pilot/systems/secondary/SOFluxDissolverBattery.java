package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public final class SOFluxDissolverBattery extends SOSecondarySystem {
    private static final float DURATION = 3f;
    private static final float VENT_MULT = 3.0f;
    private static final float FLUX_PER_MISSILE = 150f;

    private float fluxVentedThisWindow = 0f;
    private float prevFlux = 0f;

    @Override protected float baseCooldown() { return 25f; }
    @Override protected float activeDuration() { return DURATION; }
    @Override public String getDisplayName() { return "Flux Dissolver Battery"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        prevFlux = ship.getFluxTracker().getCurrFlux();
        fluxVentedThisWindow = 0f;
        ship.getMutableStats().getFluxDissipation().modifyMult("so_flux_dissolver", VENT_MULT);
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (active) {
            float currFlux = ship.getFluxTracker().getCurrFlux();
            float vented = Math.max(0f, prevFlux - currFlux);
            fluxVentedThisWindow += vented;
            prevFlux = currFlux;

            while (fluxVentedThisWindow >= FLUX_PER_MISSILE) {
                fluxVentedThisWindow -= FLUX_PER_MISSILE;
                spawnMicromissile(ship, engine);
            }

            activeTimer -= dt;
            if (activeTimer <= 0f) {
                active = false;
                ship.getMutableStats().getFluxDissipation().unmodify("so_flux_dissolver");
            }
        }
        tickCooldown(dt);
    }

    private void spawnMicromissile(ShipAPI ship, CombatEngineAPI engine) {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
            0.3f, 150f, 80f, 80f, 20f,
            com.fs.starfarer.api.combat.CollisionClass.PROJECTILE_NO_FF,
            com.fs.starfarer.api.combat.CollisionClass.PROJECTILE_FIGHTER,
            2f, 6f, 0.5f, 15,
            new Color(100, 200, 255, 200),
            new Color(50, 150, 255, 150)
        );
        spec.setDamageType(DamageType.ENERGY);
        spec.setMaxEMPDamage(200f);
        spec.setMinEMPDamage(200f);
        engine.spawnDamagingExplosion(spec, ship, new Vector2f(ship.getLocation()), false);
    }
}
