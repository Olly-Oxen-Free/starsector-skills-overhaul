package skills_overhaul.artificer.alphastorm;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.Map;

public final class AlphaStormState {

    public enum Intensity { MILD, MODERATE, SEVERE }

    public static final String K_ACTIVE    = "$skills.artificer.alpha_storm.active";
    public static final String K_START_DAY = "$skills.artificer.alpha_storm.startDay";

    public static boolean isActive(Map<String,Object> mem) {
        return Boolean.TRUE.equals(mem.get(K_ACTIVE));
    }

    public static int daysActive(Map<String,Object> mem, int today) {
        if (!isActive(mem)) return 0;
        Object start = mem.get(K_START_DAY);
        return start instanceof Number ? today - ((Number) start).intValue() : 0;
    }

    public static Intensity intensity(Map<String,Object> mem, int today) {
        if (!isActive(mem)) return null;
        int d = daysActive(mem, today);
        if (d <= 2) return Intensity.MILD;
        if (d <= 4) return Intensity.MODERATE;
        return Intensity.SEVERE;
    }

    public static int dailyCost(Intensity intensity) {
        if (intensity == null) return 0;
        switch (intensity) {
            case MILD:     return 2;
            case MODERATE: return 5;
            case SEVERE:   return 10;
            default:       return 0;
        }
    }

    public static void start(Map<String,Object> mem, int today) {
        mem.put(K_ACTIVE, true);
        mem.put(K_START_DAY, today);
    }

    public static void stop(Map<String,Object> mem) {
        mem.remove(K_ACTIVE);
        mem.remove(K_START_DAY);
    }

    // MemoryAPI bridges
    public static boolean isActive(MemoryAPI mem) {
        return Boolean.TRUE.equals(mem.get(K_ACTIVE));
    }

    public static Intensity intensity(MemoryAPI mem, int today) {
        if (!isActive(mem)) return null;
        Object start = mem.get(K_START_DAY);
        if (!(start instanceof Number)) return null;
        int d = today - ((Number) start).intValue();
        if (d <= 2) return Intensity.MILD;
        if (d <= 4) return Intensity.MODERATE;
        return Intensity.SEVERE;
    }

    public static void start(MemoryAPI mem, int today) {
        mem.set(K_ACTIVE, true);
        mem.set(K_START_DAY, today);
    }

    public static void stop(MemoryAPI mem) {
        mem.unset(K_ACTIVE);
        mem.unset(K_START_DAY);
    }

    private AlphaStormState() {}
}
