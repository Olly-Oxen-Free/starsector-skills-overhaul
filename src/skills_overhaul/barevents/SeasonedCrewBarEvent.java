package skills_overhaul.barevents;

import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;

/**
 * Seasoned Crew bar event stub (Phase 10.6).
 *
 * Intent: at a market bar, player meets a seasoned crew member who served X months.
 * Player can accept their offer: +XP to Pilot or Commander tree in exchange for
 * credits / reputation / crew slots.
 *
 * Currently UNREGISTERED. To wire in, either:
 *   1. Add a data/campaign/bar_events.csv entry + pair with a rules-based creator, OR
 *   2. Register via BarEventManager.getInstance().addEventCreator(new SeasonedCrewBarEvent())
 *      from SkillsOverhaulModPlugin.onGameLoad
 *
 * Option 2 needs dedup guard — don't add on every load, check wasAutoAdded() or a memory
 * key.
 */
public class SeasonedCrewBarEvent extends BaseBarEventCreator {
    public static final String EVENT_ID = "so_seasoned_crew";

    @Override
    public PortsideBarEvent createBarEvent() {
        // FIXME(Phase 10.6): return a BaseBarEventWithPerson subclass that drives the dialog
        // and calls LevelService to award XP on acceptance. Stub returns null = event skipped.
        return null;
    }

    @Override public String getBarEventId() { return EVENT_ID; }
    @Override public float getBarEventFrequencyWeight() { return 0.2f; }
    @Override public float getBarEventTimeoutDuration() { return 120f; }
    @Override public float getBarEventAcceptedTimeoutDuration() { return 360f; }
}
