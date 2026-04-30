package skills_overhaul.pilot.gunnery;

public final class BountyHunterState {
    private static final float FLUX_WINDOW = 8f;
    private static final float AMMO_PER_KILL = 0.25f;

    private float fluxTimer = 0f;
    private float ammoRestored = 0f;

    public void onKill() {
        fluxTimer = FLUX_WINDOW;
        ammoRestored = Math.min(1f, ammoRestored + AMMO_PER_KILL);
    }

    public void advance(float dt) { fluxTimer = Math.max(0f, fluxTimer - dt); }
    public boolean isFluxBonusActive() { return fluxTimer > 0f; }
    public float getAmmoRestoredFraction() { return ammoRestored; }
    public void reset() { fluxTimer = 0f; ammoRestored = 0f; }
}
