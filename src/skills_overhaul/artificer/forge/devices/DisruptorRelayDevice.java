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

public final class DisruptorRelayDevice implements ForgeDeviceEffect {

    public static final int RELAY_COUNT = 3;
    public static final float DURATION = 60f;
    public static final float ARC_INTERVAL = 4f;
    public static final float ARC_RANGE = 1500f;
    public static final float EMP_DAMAGE = 200f;

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        // Place 3 relays at random positions
        final Vector2f[] relays = new Vector2f[RELAY_COUNT];
        final Random rng = new Random();
        for (int i = 0; i < RELAY_COUNT; i++) {
            relays[i] = new Vector2f(
                    (rng.nextFloat() - 0.5f) * 4000f,
                    (rng.nextFloat() - 0.5f) * 4000f);
        }
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            float[] arcTimers = new float[RELAY_COUNT];
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) { engine.removePlugin(this); return; }
                for (int i = 0; i < RELAY_COUNT; i++) {
                    arcTimers[i] += amount;
                    if (arcTimers[i] < ARC_INTERVAL) continue;
                    arcTimers[i] = 0f;
                    ShipAPI nearest = nearestEnemy(engine, relays[i], ARC_RANGE);
                    if (nearest == null) continue;
                    try {
                        engine.spawnEmpArc(null, relays[i], null, nearest, DamageType.ENERGY,
                                EMP_DAMAGE, 0f, ARC_RANGE, "tachyon_lance_emp_impact",
                                10f, new java.awt.Color(120, 180, 255), java.awt.Color.WHITE);
                    } catch (Throwable t) {
                        Global.getCombatEngine().applyDamage(
                                nearest, nearest.getLocation(), EMP_DAMAGE,
                                DamageType.ENERGY, 0f, false, false, null);
                    }
                }
            }

            ShipAPI nearestEnemy(CombatEngineAPI e, Vector2f from, float maxDist) {
                ShipAPI best = null; float bestD = maxDist;
                for (ShipAPI s : e.getShips()) {
                    if (s.getOwner() != 1 || !s.isAlive()) continue;
                    float d = Misc.getDistance(from, s.getLocation());
                    if (d < bestD) { bestD = d; best = s; }
                }
                return best;
            }
        });
    }
}
