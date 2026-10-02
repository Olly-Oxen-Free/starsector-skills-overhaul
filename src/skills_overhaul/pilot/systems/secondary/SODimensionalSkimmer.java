package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public final class SODimensionalSkimmer extends SOSecondarySystem {
    private static final float TELEPORT_DIST = 800f;
    private static final float INTANGIBLE_DUR = 0.4f;
    private static final float EMP_RADIUS = 500f;

    private float intangibleTimer = 0f;
    private boolean wasPhaseShip = false;

    @Override protected float baseCooldown() { return 25f; }
    @Override protected float activeDuration() { return 0f; }
    @Override public String getDisplayName() { return "Dimensional Skimmer"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f loc = ship.getLocation();
        float facing = ship.getFacing();
        float dx = (float) Math.cos(Math.toRadians(facing)) * TELEPORT_DIST;
        float dy = (float) Math.sin(Math.toRadians(facing)) * TELEPORT_DIST;
        loc.set(loc.x + dx, loc.y + dy);

        // Phase-class ships: teleport only, no forced-phase (already can phase)
        wasPhaseShip = isPhaseShip(ship);
        if (!wasPhaseShip) {
            intangibleTimer = INTANGIBLE_DUR;
            ship.setPhased(true);
        }

        DamagingExplosionSpec spec = new DamagingExplosionSpec(
            0.3f, 250f, EMP_RADIUS, 0f, 0f,
            CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER,
            3f, 10f, 0.5f, 20,
            new Color(150, 200, 255, 200), new Color(100, 150, 255, 100)
        );
        spec.setDamageType(DamageType.ENERGY);
        spec.setMaxEMPDamage(400f);
        spec.setMinEMPDamage(200f);
        engine.spawnDamagingExplosion(spec, ship, new Vector2f(ship.getLocation()), false);
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (intangibleTimer > 0f) {
            intangibleTimer -= dt;
            if (intangibleTimer <= 0f) ship.setPhased(false);
        }
        tickCooldown(dt);
    }

    private boolean isPhaseShip(ShipAPI ship) {
        // Phase cloak is the hull's defense, not getSystem(); vanilla idiom.
        return ship.getHullSpec() != null && ship.getHullSpec().isPhase();
    }
}
