package skills_overhaul.xp;

import java.util.HashMap;
import java.util.Map;

public class DamageLedger {
    public static class Entry {
        public float directManual, directAuto;
        public float fighterManual, fighterAuto;
        public float totalToShip;
    }

    private final Map<String, Entry> byShip = new HashMap<>();

    private Entry getOrCreate(String id) {
        Entry e = byShip.get(id);
        if (e == null) { e = new Entry(); byShip.put(id, e); }
        return e;
    }

    public void addDirect(String shipId, float amount, boolean autopilotOn) {
        Entry e = getOrCreate(shipId);
        if (autopilotOn) e.directAuto += amount; else e.directManual += amount;
    }
    public void addFighter(String shipId, float amount, boolean autopilotOn) {
        Entry e = getOrCreate(shipId);
        if (autopilotOn) e.fighterAuto += amount; else e.fighterManual += amount;
    }
    public void addTotal(String shipId, float amount) {
        Entry e = getOrCreate(shipId);
        e.totalToShip += amount;
    }

    public float effectiveDirectShare(String shipId, float autopilotMult) {
        Entry e = byShip.get(shipId);
        if (e == null || e.totalToShip <= 0f) return 0f;
        return (autopilotMult * e.directAuto + e.directManual) / e.totalToShip;
    }

    public float effectiveFighterShare(String shipId, float autopilotMult) {
        Entry e = byShip.get(shipId);
        if (e == null || e.totalToShip <= 0f) return 0f;
        return (autopilotMult * e.fighterAuto + e.fighterManual) / e.totalToShip;
    }
}
