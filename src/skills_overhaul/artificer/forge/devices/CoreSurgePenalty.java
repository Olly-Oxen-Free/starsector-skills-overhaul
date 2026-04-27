package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class CoreSurgePenalty implements ForgeDevicePenalty {
    @Override
    public void apply(final CombatEngineAPI engine, ShipAPI flagship) {
        // Fleet-wide system regen ×0.50 for rest of battle
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            try {
                s.getMutableStats().getSystemRegenBonus().modifyMult(
                        "forge_core_surge_penalty", 0.50f);
            } catch (Throwable t) {}
        }
    }
}
