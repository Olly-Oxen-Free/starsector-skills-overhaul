package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public interface ForgeDevicePenalty {
    void apply(CombatEngineAPI engine, ShipAPI flagship);
}
