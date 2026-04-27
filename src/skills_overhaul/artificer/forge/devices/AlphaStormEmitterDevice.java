package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import skills_overhaul.artificer.alphastorm.AlphaStormLightning;
import skills_overhaul.artificer.alphastorm.AlphaStormState;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import java.util.List;

public final class AlphaStormEmitterDevice implements ForgeDeviceEffect {

    public static final float RADIUS = 2000f;
    public static final String STAT_KEY = "forge_alpha_storm_emitter";

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        // If campaign Alpha Storm is also active, force severe; else moderate
        boolean campaignActive = AlphaStormState.isActive(
                Global.getSector().getMemoryWithoutUpdate());
        final AlphaStormState.Intensity intensity = campaignActive
                ? AlphaStormState.Intensity.SEVERE
                : AlphaStormState.Intensity.MODERATE;
        final Vector2f emitter = new Vector2f(0f, 0f);

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override public void advance(float amount, List<InputEventAPI> events) {
                for (ShipAPI s : engine.getShips()) {
                    if (!s.isAlive()) continue;
                    if (Misc.getDistance(emitter, s.getLocation()) > RADIUS) continue;
                    AlphaStormLightning.rollAndStrike(engine, s, intensity, amount);
                }
            }
        });
    }
}
