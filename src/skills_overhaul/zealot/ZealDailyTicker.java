package skills_overhaul.zealot;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class ZealDailyTicker implements EveryFrameScript {

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        int today = monotonicDay();
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Map<String, Object> wrapper = new MemoryWrapMap(mem);
        ZealService.advanceDay(wrapper, today, 0f);
    }

    public static int monotonicDay() {
        return (int) Global.getSector().getClock().getCycle() * 365
                + (int) Global.getSector().getClock().getDay();
    }

    /**
     * Map view backed by a MemoryAPI. get reads from MemoryAPI; put writes to MemoryAPI; remove unsets.
     * Other Map methods are minimal stubs.
     */
    static final class MemoryWrapMap extends AbstractMap<String, Object> {
        private final MemoryAPI mem;

        MemoryWrapMap(MemoryAPI mem) { this.mem = mem; }

        @Override
        public Object get(Object key) { return mem.get((String) key); }

        @Override
        public Object put(String key, Object value) { mem.set(key, value); return value; }

        @Override
        public Object remove(Object key) { mem.unset((String) key); return null; }

        @Override
        public boolean containsKey(Object key) {
            return mem.get((String) key) != null;
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return Collections.emptySet();
        }
    }
}
