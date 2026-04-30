package skills_overhaul.pilot.defense;

public final class AdamantineState {
    private static final float BUFFER_FRACTION = 0.20f;
    private static final float DISCHARGE_COOLDOWN = 5f;

    private final float maxFlux;
    private float buffer = 0f;
    private float dischargeCooldown = 0f;

    public AdamantineState(float maxFlux) { this.maxFlux = maxFlux; }

    public void updateFlux(float currFlux) {
        buffer = Math.min(maxFlux * BUFFER_FRACTION, currFlux);
    }

    /** Returns true if buffer was exhausted (discharge triggered). */
    public boolean absorbHullDamage(float damage) {
        if (buffer <= 0f || dischargeCooldown > 0f) return false;
        buffer -= damage;
        if (buffer <= 0f) {
            buffer = 0f;
            dischargeCooldown = DISCHARGE_COOLDOWN;
            return true;
        }
        return false;
    }

    public void advance(float dt) {
        if (dischargeCooldown > 0f) dischargeCooldown = Math.max(0f, dischargeCooldown - dt);
    }

    public float getBuffer() { return buffer; }
    public boolean canDischarge() { return dischargeCooldown <= 0f; }
}
