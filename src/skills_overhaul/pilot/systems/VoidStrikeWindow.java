package skills_overhaul.pilot.systems;

public final class VoidStrikeWindow {
    private static final float WINDOW = 3f;

    private final boolean isPhaseShip;
    private float remaining = 0f;

    public VoidStrikeWindow(boolean isPhaseShip) { this.isPhaseShip = isPhaseShip; }

    public void trigger() { if (remaining <= 0f) remaining = WINDOW; }
    public void advance(float dt) { remaining = Math.max(0f, remaining - dt); }
    public boolean isActive() { return remaining > 0f; }

    public float getOrdnanceDamageBonus()   { return isPhaseShip ? 0.35f : 0.30f; }
    public float getAllWeaponDamageBonus()   { return isPhaseShip ? 0.20f : 0.15f; }
    public float getOrdnanceSpeedBonus()    { return isPhaseShip ? 0.25f : 0.20f; }
    public void reset() { remaining = 0f; }
}
