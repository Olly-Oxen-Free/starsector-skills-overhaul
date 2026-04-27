package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class PulsarArrayPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Player shields within beam path take +50% damage — flag consumed by PulsarArrayDevice plugin
        com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.pulsar_array_friendly_fire", true);
    }
}
