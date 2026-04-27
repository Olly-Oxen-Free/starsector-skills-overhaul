package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

import java.util.List;

public final class TransverseVentPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(final CombatEngineAPI engine, ShipAPI flagship) {
        // Fleet flux dissipation ×0.70 for 30s
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            s.getMutableStats().getFluxDissipation().modifyMult("forge_transverse_vent_penalty", 0.70f);
        }
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = 30f;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) {
                    for (ShipAPI s : engine.getShips()) {
                        if (s.getOwner() != 0) continue;
                        s.getMutableStats().getFluxDissipation().unmodify("forge_transverse_vent_penalty");
                    }
                    engine.removePlugin(this);
                }
            }
        });
    }
}
