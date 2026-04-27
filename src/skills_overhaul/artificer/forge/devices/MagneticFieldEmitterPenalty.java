package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class MagneticFieldEmitterPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Zone affects player ships too (no friend-foe filter) — flag consumed by MagneticFieldEmitterDevice plugin
        com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.magfield_affects_friendly", true);
    }
}
