package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import java.util.List;

public final class ECMWaveDevice implements ForgeDeviceEffect {

    public static final float DURATION = 60f;
    public static final float DISABLE_DURATION = 5f;

    @Override
    public void deployBattle(final CombatEngineAPI engine, boolean overQuota) {
        // Initial weapon-disable pulse on enemies
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 1) continue;
            for (WeaponAPI w : s.getAllWeapons()) {
                try { w.disable(); } catch (Throwable t) {}
            }
        }
        // 60s ECM amplification — applied per-friendly-ship via stat
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            try {
                s.getMutableStats().getDynamic().getMod("ew_jamming")
                        .modifyMult("forge_ecm_wave", 1.20f);
            } catch (Throwable t) {}
        }
        // After 60s: unmodify
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                remaining -= amount;
                if (remaining <= 0f) {
                    for (ShipAPI s : engine.getShips()) {
                        if (s.getOwner() != 0) continue;
                        try {
                            s.getMutableStats().getDynamic().getMod("ew_jamming").unmodify("forge_ecm_wave");
                        } catch (Throwable t) {}
                    }
                    engine.removePlugin(this);
                }
            }
        });
    }
}
