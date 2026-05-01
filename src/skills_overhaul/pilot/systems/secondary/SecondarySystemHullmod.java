package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public interface SecondarySystemHullmod {
    void activate(ShipAPI ship, CombatEngineAPI engine);
    boolean isOnCooldown();
    void advance(float dt, ShipAPI ship, CombatEngineAPI engine);
    float getCooldownRemaining();
    float getMaxCooldown();
    String getDisplayName();
}
