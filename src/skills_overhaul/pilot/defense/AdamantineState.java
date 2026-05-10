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
        // Spec §5.4 line 39: Buffer = min(currFlux, maxFlux × 0.20).
        // "Recharges as flux rebuilds" via lastKnownFlux delta tracking
        // is only relevant when buffer drops via damage absorption mid-combat.
        buffer = Math.min(maxFlux * BUFFER_FRACTION, currFlux);
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
