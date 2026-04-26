package skills_overhaul.corsair;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public final class InfamyDailyTicker implements EveryFrameScript {

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        // getInfamy() side-effects rollTo; result discarded.
        InfamyService.getInfamy(Global.getSector().getMemoryWithoutUpdate(), monotonicDay());
    }

    /** Monotonic int day count (cycle*365 + day). */
    public static int monotonicDay() {
        return (int) Global.getSector().getClock().getCycle() * 365
                + (int) Global.getSector().getClock().getDay();
    }
}
