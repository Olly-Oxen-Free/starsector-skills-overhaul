package skills_overhaul.pilot.maneuvering;

public final class VentTracker {
    private static final float DEEP_VENT_WINDOW = 2f;

    private boolean wasVenting = false;
    private float fluxAtVentStart = 0f;
    private float maxFluxAtVentStart = 1f;
    private float ventElapsed = 0f;
    private boolean currentlyVenting = false;

    public void advance(float dt, boolean isVenting, float currFlux, float maxFlux) {
        if (isVenting && !wasVenting) {
            fluxAtVentStart = currFlux;
            maxFluxAtVentStart = maxFlux > 0f ? maxFlux : 1f;
            ventElapsed = 0f;
        }
        if (isVenting) ventElapsed += dt;
        if (!isVenting) ventElapsed = 0f;
        currentlyVenting = isVenting;
        wasVenting = isVenting;
    }

    public float getVentSprintSpeedBonus() {
        if (!currentlyVenting || maxFluxAtVentStart <= 0f) return 0f;
        return (fluxAtVentStart / maxFluxAtVentStart) * 0.15f;
    }

    public float getVentSprintManeuverBonus() {
        if (!currentlyVenting || maxFluxAtVentStart <= 0f) return 0f;
        return (fluxAtVentStart / maxFluxAtVentStart) * 0.60f;
    }

    public boolean isDeepVentActive() {
        return currentlyVenting && ventElapsed <= DEEP_VENT_WINDOW;
    }

    public boolean isVenting() { return currentlyVenting; }
}
