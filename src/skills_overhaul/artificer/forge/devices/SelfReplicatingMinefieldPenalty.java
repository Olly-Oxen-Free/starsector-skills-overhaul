package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class SelfReplicatingMinefieldPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Mines also damage friendly ships — handled by removing friend-foe filter on mine targeting.
        // v1: log; mine targeting friend-foe filter removal needs custom mine ship behavior — TODO.
        com.fs.starfarer.api.Global.getLogger(SelfReplicatingMinefieldPenalty.class).info(
                "[Minefield] over-quota penalty: friendly damage enabled");
    }
}
