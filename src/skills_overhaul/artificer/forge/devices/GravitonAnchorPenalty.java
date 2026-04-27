package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class GravitonAnchorPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Player ships within 2000u also slowed — apply per-frame via plugin
        com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate().set(
                "$skills.artificer.forge.graviton_self_slow", true);
    }
}
