package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/**
 * Precision Strike (Systems T4, {@code so_systems_precision_strike}).
 *
 * Spec: per shot compute {@code rangeFrac = distToTarget / weapon.getRange()};
 * {@code penetBonus = max(0, 0.25 * (1 - rangeFrac / 0.40))} — a range-scaled
 * armor-penetration bonus that peaks at +25% point-blank and fades to 0 at 40%
 * of the weapon's range. Energy weapons only.
 *
 * IMPLEMENTATION NOTE / judgment call: the spec asks to "reduce the target's
 * armor rating in the damage formula." The public {@link DamageAPI} exposes no
 * hook to lower the armor rating actually used by the armor-damage equation
 * (there is no per-hit armor-rating field). We therefore approximate armor
 * penetration as a range-scaled bonus damage multiplier applied to energy hits
 * on non-shield surfaces: {@code damage.getModifier().modifyMult(1 + penetBonus)}.
 * This is a "more damage through armor at close range" reading rather than a
 * literal armor-rating reduction; magnitudes match the spec's 0..25% envelope.
 */
public final class PrecisionStrikeListener implements DamageDealtModifier {

    private static final String KEY = "so_systems_precision_strike";
    private static final float MAX_BONUS = 0.25f;
    private static final float RANGE_CUTOFF_FRAC = 0.40f;

    private final ShipAPI ship;

    public PrecisionStrikeListener(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target,
                                    DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || damage == null || point == null) return null;

        WeaponAPI weapon;
        ShipAPI source;
        if (param instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            weapon = proj.getWeapon();
            source = proj.getSource();
        } else if (param instanceof BeamAPI) {
            BeamAPI beam = (BeamAPI) param;
            weapon = beam.getWeapon();
            source = beam.getSource();
        } else {
            return null;
        }

        if (source != ship) return null;
        if (weapon == null || weapon.getType() != WeaponAPI.WeaponType.ENERGY) return null;

        float range = weapon.getRange();
        if (range <= 0f) return null;

        float dist = Misc.getDistance(source.getLocation(), point);
        float rangeFrac = dist / range;
        float penetBonus = Math.max(0f, MAX_BONUS * (1f - rangeFrac / RANGE_CUTOFF_FRAC));
        if (penetBonus <= 0f) return null;

        damage.getModifier().modifyMult(KEY, 1f + penetBonus);
        return KEY;
    }
}
