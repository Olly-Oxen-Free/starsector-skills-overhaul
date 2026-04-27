package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class ECMWavePenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Player ships' weapons disabled for 1s on deploy
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            for (WeaponAPI w : s.getAllWeapons()) {
                try { w.disable(); } catch (Throwable t) {}
            }
        }
    }
}
