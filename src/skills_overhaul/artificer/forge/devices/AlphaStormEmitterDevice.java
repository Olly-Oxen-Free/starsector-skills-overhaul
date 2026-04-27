package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;
import java.util.Random;

/**
 * Alpha Storm Emitter — Plan D infrastructure (deferred).
 * v1 stub: spawns localized lightning + ECM amp at 2000u radius, MODERATE intensity.
 * Penalty: lightning also strikes player ships.
 *
 * TODO (Plan D): full alpha-storm terrain, intensity scaling, strike patterns.
 */
public final class AlphaStormEmitterDevice implements ForgeDeviceEffect {

    public static final float DURATION = 60f;
    public static final float RADIUS = 2000f;
    public static final float STRIKE_INTERVAL = 3f;
    public static final float EMP_DAMAGE = 150f;
    private static final String ECM_KEY = "forge_alpha_storm_ecm";

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        final Vector2f center = new Vector2f(0f, 0f);
        final Random rng = new Random();

        // ECM amplification on friendly ships
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isAlive()) continue;
            try {
                s.getMutableStats().getDynamic().getMod("ew_jamming")
                        .modifyMult(ECM_KEY, 1.30f);
            } catch (Throwable t) {}
        }

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            float strikeTimer = 0f;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) {
                    for (ShipAPI s : engine.getShips()) {
                        try { s.getMutableStats().getDynamic().getMod("ew_jamming").unmodify(ECM_KEY); } catch (Throwable t) {}
                    }
                    engine.removePlugin(this);
                    return;
                }
                strikeTimer += amount;
                if (strikeTimer < STRIKE_INTERVAL) return;
                strikeTimer = 0f;
                // Lightning strike: pick random enemy within radius
                ShipAPI target = null;
                float bestD = RADIUS;
                for (ShipAPI s : engine.getShips()) {
                    if (s.getOwner() != 1 || !s.isAlive()) continue;
                    float d = Misc.getDistance(center, s.getLocation());
                    if (d < bestD) { bestD = d; target = s; }
                }
                if (target == null) return;
                Vector2f strikeOrigin = new Vector2f(
                        center.x + (rng.nextFloat() - 0.5f) * RADIUS,
                        center.y + (rng.nextFloat() - 0.5f) * RADIUS);
                try {
                    engine.spawnEmpArc(null, strikeOrigin, null, target, DamageType.ENERGY,
                            EMP_DAMAGE, 0f, RADIUS, "tachyon_lance_emp_impact",
                            15f, new java.awt.Color(200, 220, 255), java.awt.Color.WHITE);
                } catch (Throwable t) {
                    Global.getLogger(AlphaStormEmitterDevice.class).warn(
                            "[AlphaStorm] strike failed: " + t.getMessage());
                }
            }
        });
    }
}
