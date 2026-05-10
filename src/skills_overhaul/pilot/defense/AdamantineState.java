package skills_overhaul.pilot.defense;

public final class AdamantineState {
    private static final float BUFFER_FRACTION = 0.20f;
    private static final float DISCHARGE_COOLDOWN = 5f;

    private static final float POST_DISCHARGE_DURATION = 5f;

    private final float maxFlux;
    private float buffer = 0f;
    private float dischargeCooldown = 0f;
    private float postDischargeTimer = 0f;
    private float lastKnownFlux = 0f;  // spec line 39: track delta for buffer recharge

    public AdamantineState(float maxFlux) { this.maxFlux = maxFlux; }

    public void updateFlux(float currFlux) {
        // spec §5.4 line 39: buffer recharges as flux climbs (track delta).
        // Buffer is min(currFlux × 1.0, maxFlux × 0.20).
        float fluxDelta = Math.max(0f, currFlux - lastKnownFlux);
        buffer = Math.min(maxFlux * BUFFER_FRACTION, buffer + fluxDelta);
        lastKnownFlux = currFlux;
    }

    public void startPostDischarge() { postDischargeTimer = POST_DISCHARGE_DURATION; }
    public boolean isPostDischargeActive() { return postDischargeTimer > 0f; }

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
        if (postDischargeTimer > 0f) postDischargeTimer = Math.max(0f, postDischargeTimer - dt);
    }

    public float getBuffer() { return buffer; }
    public boolean canDischarge() { return dischargeCooldown <= 0f; }
}
