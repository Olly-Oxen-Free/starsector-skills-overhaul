package skills_overhaul.corsair;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public final class InfamyDailyTicker implements EveryFrameScript {

    private int lastCheckedDay = Integer.MIN_VALUE;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        // Rotation is daily — no need to hit memory every frame.
        int day = monotonicDay();
        if (day == lastCheckedDay) return;
        lastCheckedDay = day;
        // getInfamy() side-effects rollTo; result discarded.
        InfamyService.getInfamy(Global.getSector().getMemoryWithoutUpdate(), day);
    }

    /**
     * Monotonic int day count (cycle*365 + day-of-year). getDay() is the day of the MONTH, so
     * the old cycle*365 + getDay() went backwards at every month rollover.
     */
    public static int monotonicDay() {
        return (int) Global.getSector().getClock().getCycle() * 365
                + Global.getSector().getClock().getCal().get(java.util.Calendar.DAY_OF_YEAR);
    }
}
