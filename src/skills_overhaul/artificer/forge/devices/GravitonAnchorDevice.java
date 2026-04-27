package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;

public final class GravitonAnchorDevice implements ForgeDeviceEffect {

    public static final float DURATION = 90f;
    public static final float RADIUS = 2000f;

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        final Vector2f anchorPos = new Vector2f(0f, 0f);
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) {
                    for (ShipAPI s : engine.getShips()) {
                        s.getMutableStats().getMaxSpeed().unmodify("forge_graviton_anchor");
                        s.getMutableStats().getMaxTurnRate().unmodify("forge_graviton_anchor");
                    }
                    engine.removePlugin(this);
                    return;
                }
                for (ShipAPI s : engine.getShips()) {
                    if (s.getOwner() != 1 || !s.isAlive()) continue;
                    if (Misc.getDistance(anchorPos, s.getLocation()) > RADIUS) {
                        s.getMutableStats().getMaxSpeed().unmodify("forge_graviton_anchor");
                        s.getMutableStats().getMaxTurnRate().unmodify("forge_graviton_anchor");
                        continue;
                    }
                    s.getMutableStats().getMaxSpeed().modifyMult("forge_graviton_anchor", 0.70f);
                    s.getMutableStats().getMaxTurnRate().modifyMult("forge_graviton_anchor", 0.50f);
                }
            }
        });
    }
}
