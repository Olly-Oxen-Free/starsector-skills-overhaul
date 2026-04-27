package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class AlphaStormEmitterPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Lightning also strikes player ships — flag consumed by AlphaStormEmitterDevice plugin
        com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.alpha_storm_friendly_strikes", true);
    }
}
