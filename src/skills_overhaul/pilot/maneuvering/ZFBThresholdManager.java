package skills_overhaul.pilot.maneuvering;

public final class ZFBThresholdManager {
    private float threshold = 0.20f;
    private boolean wasAboveThreshold = true;
    private boolean surgeFired = false;

    public void applyZeroFluxMastery() { threshold = Math.max(threshold, 0.35f); }
    public void applyUntouchable()     { threshold = 0.50f; }
    public float getThreshold()        { return threshold; }
    public boolean isInZFB(float fluxFrac) { return fluxFrac < threshold; }

    public boolean checkZFMSurge(float fluxFrac) {
        boolean above = fluxFrac >= threshold;
        if (above) {
            wasAboveThreshold = true;
            surgeFired = false;
            return false;
        }
        if (wasAboveThreshold && !surgeFired) {
            wasAboveThreshold = false;
            surgeFired = true;
            return true;
        }
        wasAboveThreshold = false;
        return false;
    }

    public void reset() { threshold = 0.20f; wasAboveThreshold = true; surgeFired = false; }
}
