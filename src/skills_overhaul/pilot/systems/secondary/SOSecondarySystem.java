package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public abstract class SOSecondarySystem implements SecondarySystemHullmod {
    protected float cooldownRemaining = 0f;
    protected boolean active = false;
    protected float activeTimer = 0f;

    protected abstract float baseCooldown();
    protected abstract float activeDuration();

    @Override public boolean isOnCooldown() { return cooldownRemaining > 0f; }
    @Override public float getCooldownRemaining() { return cooldownRemaining; }
    @Override public float getMaxCooldown() { return baseCooldown(); }

    protected void tickCooldown(float dt) {
        if (cooldownRemaining > 0f) cooldownRemaining = Math.max(0f, cooldownRemaining - dt);
    }

    protected void startActivation() {
        cooldownRemaining = baseCooldown();
        if (activeDuration() > 0f) { active = true; activeTimer = activeDuration(); }
    }

    @Override
    public void activate(ShipAPI ship, CombatEngineAPI engine) {
        if (isOnCooldown()) return;
        onActivate(ship, engine);
        startActivation();
    }

    protected abstract void onActivate(ShipAPI ship, CombatEngineAPI engine);
}
