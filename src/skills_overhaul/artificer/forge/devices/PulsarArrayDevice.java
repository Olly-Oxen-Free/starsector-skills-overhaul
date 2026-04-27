package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;

/**
 * Pulsar Array — spawns a pulsar beam from a random map edge across to the opposite edge.
 * Per-frame: enemy ships in beam path take 200 energy/s + 100 EMP/s.
 * Penalty: player shields within beam path take +50% damage.
 *
 * TODO: fill beam-path collision using MCTE pulsar pattern as reference.
 * v1 skeleton: per-frame damage applied to enemies within BEAM_HALF_WIDTH of beam axis.
 */
public final class PulsarArrayDevice implements ForgeDeviceEffect {

    public static final float DURATION = 45f;
    public static final float BEAM_HALF_WIDTH = 300f;
    public static final float ENERGY_DPS = 200f;
    public static final float EMP_DPS = 100f;

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        // Beam axis: random map edge to opposite edge (horizontal or vertical crossing)
        final boolean horizontal = (Math.random() > 0.5);
        final float mapW = engine.getMapWidth();
        final float mapH = engine.getMapHeight();
        final float beamCoord = (float) ((Math.random() - 0.5) * (horizontal ? mapH : mapW));

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) { engine.removePlugin(this); return; }
                for (ShipAPI s : engine.getShips()) {
                    if (s.getOwner() != 1 || !s.isAlive()) continue;
                    float axisCoord = horizontal ? s.getLocation().y : s.getLocation().x;
                    if (Math.abs(axisCoord - beamCoord) > BEAM_HALF_WIDTH) continue;
                    try {
                        engine.applyDamage(s, s.getLocation(),
                                ENERGY_DPS * amount, DamageType.ENERGY, EMP_DPS * amount,
                                false, false, null);
                    } catch (Throwable t) {
                        Global.getLogger(PulsarArrayDevice.class).warn(
                                "[PulsarArray] damage apply failed: " + t.getMessage());
                    }
                }
            }
        });
    }
}
