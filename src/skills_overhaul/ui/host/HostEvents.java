package skills_overhaul.ui.host;

/**
 * Outcomes a host reports instead of logging or routing them itself. The receiver (5c's
 * router) decides the fallback and writes the single WARN/INFO line.
 */
public interface HostEvents {

    /** Second-in-Command owns the skill panel; the host did not mount. At most once per arming. */
    void onSicDetected();

    /** The deferred mount gave up; lastReason is the last probe failure. Once per arming. */
    void onProbeExhausted(String lastReason);
}
