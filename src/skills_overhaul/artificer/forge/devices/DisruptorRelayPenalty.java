package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class DisruptorRelayPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // 10% of relay arcs target friendly ships — flag for plugin
        com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.disruptor_relay_friendly_fire", 0.10f);
    }
}
