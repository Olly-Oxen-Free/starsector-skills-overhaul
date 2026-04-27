package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import java.util.List;

public final class TransverseVentDevice implements ForgeDeviceEffect {

    public static final float RECOVERY_DURATION = 5f;

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        // Vent all hard flux on friendly ships
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            if (s.getFluxTracker() != null) s.getFluxTracker().setHardFlux(0f);
        }
        // 5s shield damage taken ×0.50
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            s.getMutableStats().getShieldDamageTakenMult().modifyMult("forge_transverse_vent", 0.50f);
        }
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = RECOVERY_DURATION;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) {
                    for (ShipAPI s : engine.getShips()) {
                        if (s.getOwner() != 0) continue;
                        s.getMutableStats().getShieldDamageTakenMult().unmodify("forge_transverse_vent");
                    }
                    engine.removePlugin(this);
                }
            }
        });
    }
}
