package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class SOReflectorProtocol extends SOSecondarySystem {
    private static final float DURATION = 3f;
    private static final float ARC = 240f;
    private static final float REFLECT_RADIUS = 350f;
    private static final float BONUS_THRESHOLD = 800f;

    private float reflectedDamage = 0f;
    private float bonusTimer = 0f;

    @Override protected float baseCooldown() { return 30f; }
    @Override protected float activeDuration() { return DURATION; }
    @Override public String getDisplayName() { return "Reflector Protocol"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        reflectedDamage = 0f;
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (active) {
            List<DamagingProjectileAPI> toRemove = new ArrayList<DamagingProjectileAPI>();
            for (DamagingProjectileAPI proj : engine.getProjectiles()) {
                if (proj.getOwner() == ship.getOwner()) continue;
                float dist = Misc.getDistance(ship.getLocation(), proj.getLocation());
                if (dist > REFLECT_RADIUS) continue;
                float angle = Misc.getAngleInDegrees(ship.getLocation(), proj.getLocation());
                float diff = Math.abs(Misc.getAngleDiff(ship.getFacing(), angle));
                if (diff > ARC / 2f) continue;

                reflectedDamage += proj.getDamageAmount();
                toRemove.add(proj);
                DamagingExplosionSpec spec = new DamagingExplosionSpec(
                    0.2f, 60f, 80f,
                    proj.getDamageAmount(), proj.getDamageAmount() * 0.5f,
                    CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER,
                    1f, 4f, 0.3f, 8,
                    new Color(255, 200, 100, 200), new Color(255, 150, 50, 150)
                );
                spec.setDamageType(DamageType.ENERGY);
                engine.spawnDamagingExplosion(spec, ship, new Vector2f(proj.getLocation()), false);
            }
            for (DamagingProjectileAPI p : toRemove) engine.removeEntity(p);

            activeTimer -= dt;
            if (activeTimer <= 0f) {
                active = false;
                if (reflectedDamage >= BONUS_THRESHOLD) {
                    bonusTimer = 6f;
                    ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("so_reflector", 1.30f);
                    ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("so_reflector", 1.30f);
                    ship.getMutableStats().getBallisticRoFMult().modifyMult("so_reflector_rof", 1.20f);
                    ship.getMutableStats().getEnergyRoFMult().modifyMult("so_reflector_rof", 1.20f);
                }
            }
        }

        if (bonusTimer > 0f) {
            bonusTimer -= dt;
            if (bonusTimer <= 0f) {
                ship.getMutableStats().getBallisticWeaponDamageMult().unmodify("so_reflector");
                ship.getMutableStats().getEnergyWeaponDamageMult().unmodify("so_reflector");
                ship.getMutableStats().getBallisticRoFMult().unmodify("so_reflector_rof");
                ship.getMutableStats().getEnergyRoFMult().unmodify("so_reflector_rof");
            }
        }
        tickCooldown(dt);
    }
}
