package skills_overhaul.support;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.lwjgl.util.vector.Vector2f;

public final class InvincibilityListener implements HullDamageAboutToBeTakenListener {

    private final ShipAPI ship;
    private float remaining;

    public InvincibilityListener(ShipAPI ship, float duration) {
        this.ship = ship;
        this.remaining = duration;
    }

    public void advance(float amount) {
        if (remaining <= 0f) return;
        remaining -= amount;
        if (remaining <= 0f) {
            ship.removeListener(this);
        }
    }

    public boolean isExpired() { return remaining <= 0f; }
    public float getRemaining() { return remaining; }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship,
                                               Vector2f point, float damageAmount) {
        return remaining > 0f;
    }
}
