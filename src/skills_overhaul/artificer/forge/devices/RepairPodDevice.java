package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;

public final class RepairPodDevice implements ForgeDeviceEffect {

    public static final float DURATION = 60f;
    public static final float RADIUS = 1500f;
    public static final float REPAIR_RATE = 0.005f;

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        // Spawn at center of map (battle center is approximately origin)
        final Vector2f center = new Vector2f(0f, 0f);
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) { engine.removePlugin(this); return; }
                for (ShipAPI s : engine.getShips()) {
                    if (s.getOwner() != 0 || !s.isAlive()) continue;
                    if (Misc.getDistance(center, s.getLocation()) > RADIUS) continue;
                    s.setHitpoints(Math.min(s.getMaxHitpoints(),
                            s.getHitpoints() + s.getMaxHitpoints() * REPAIR_RATE * amount));
                }
            }
        });
    }
}
