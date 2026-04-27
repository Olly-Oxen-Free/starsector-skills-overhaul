package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

public final class CoreSurgeDevice implements ForgeDeviceEffect {

    @Override
    public void deployBattle(CombatEngineAPI engine, boolean overQuota) {
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isAlive()) continue;
            ShipSystemAPI sys = s.getSystem();
            if (sys == null) continue;
            try {
                int cur = sys.getAmmo();
                int max = sys.getMaxAmmo();
                if (max > 0) sys.setAmmo(Math.min(max, cur + 1));
            } catch (Throwable t) {}
        }
    }
}
