package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public final class SOCombatSnapshot extends SOSecondarySystem {
    private static final float REVERT_WINDOW = 8f;

    private float savedFlux = 0f;
    private Vector2f savedLocation = null;
    private float savedFacing = 0f;
    private float revertTimer = 0f;
    private boolean awaitingRevert = false;

    @Override protected float baseCooldown() { return 55f; }
    @Override protected float activeDuration() { return 0f; }
    @Override public String getDisplayName() { return "Combat Snapshot"; }

    @Override
    public void activate(ShipAPI ship, CombatEngineAPI engine) {
        if (awaitingRevert) {
            executeRevert(ship);
            awaitingRevert = false;
            revertTimer = 0f;
            cooldownRemaining = baseCooldown();
            return;
        }
        if (isOnCooldown()) return;
        onActivate(ship, engine);
        // Do NOT start cooldown yet; it starts after revert executes
    }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        savedFlux = ship.getFluxTracker().getCurrFlux();
        savedLocation = new Vector2f(ship.getLocation());
        savedFacing = ship.getFacing();
        revertTimer = REVERT_WINDOW;
        awaitingRevert = true;
    }

    private void executeRevert(ShipAPI ship) {
        ship.getFluxTracker().setCurrFlux(savedFlux);
        if (savedLocation != null) {
            ship.getLocation().set(savedLocation.x, savedLocation.y);
            ship.setFacing(savedFacing);
        }
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (awaitingRevert) {
            revertTimer -= dt;
            if (revertTimer <= 0f) {
                executeRevert(ship);
                awaitingRevert = false;
                cooldownRemaining = baseCooldown();
            }
        }
        tickCooldown(dt);
    }
}
