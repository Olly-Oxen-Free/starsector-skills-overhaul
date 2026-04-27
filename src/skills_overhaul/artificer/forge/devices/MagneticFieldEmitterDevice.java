package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;

/**
 * Magnetic Field Emitter — spawns a 1500u magnetic-field zone at map center.
 * Per-frame: ships within zone — missile accuracy x0.50, beam damage x0.85, energy damage x0.90.
 * Penalty: zone affects player ships too (no friend-foe filter).
 *
 * TODO: adapt MCTE magnetic-field pattern for full visual terrain effect.
 * v1 skeleton: applies stat mods per-frame to all ships within ZONE_RADIUS.
 */
public final class MagneticFieldEmitterDevice implements ForgeDeviceEffect {

    public static final float DURATION = 90f;
    public static final float ZONE_RADIUS = 1500f;
    private static final String KEY = "forge_magfield";

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        final Vector2f center = new Vector2f(0f, 0f);
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) {
                    for (ShipAPI s : engine.getShips()) {
                        try {
                            s.getMutableStats().getMissileWeaponRangeBonus().unmodify(KEY);
                            s.getMutableStats().getBeamWeaponDamageMult().unmodify(KEY);
                            s.getMutableStats().getEnergyWeaponDamageMult().unmodify(KEY);
                        } catch (Throwable t) {}
                    }
                    engine.removePlugin(this);
                    return;
                }
                for (ShipAPI s : engine.getShips()) {
                    if (!s.isAlive()) continue;
                    boolean inZone = Misc.getDistance(center, s.getLocation()) <= ZONE_RADIUS;
                    try {
                        if (inZone) {
                            s.getMutableStats().getMissileWeaponRangeBonus().modifyMult(KEY, 0.50f);
                            s.getMutableStats().getBeamWeaponDamageMult().modifyMult(KEY, 0.85f);
                            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult(KEY, 0.90f);
                        } else {
                            s.getMutableStats().getMissileWeaponRangeBonus().unmodify(KEY);
                            s.getMutableStats().getBeamWeaponDamageMult().unmodify(KEY);
                            s.getMutableStats().getEnergyWeaponDamageMult().unmodify(KEY);
                        }
                    } catch (Throwable t) {
                        Global.getLogger(MagneticFieldEmitterDevice.class).warn(
                                "[MagField] stat mod failed: " + t.getMessage());
                    }
                }
            }
        });
    }
}
