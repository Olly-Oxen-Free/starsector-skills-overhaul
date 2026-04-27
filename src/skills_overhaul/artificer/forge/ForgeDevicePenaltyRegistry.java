package skills_overhaul.artificer.forge;

import java.util.HashMap;
import java.util.Map;

public final class ForgeDevicePenaltyRegistry {
    private static final Map<String, ForgeDevicePenalty> REG = new HashMap<>();

    public static void register(String key, ForgeDevicePenalty p) {
        REG.put(key, p);
    }

    public static ForgeDevicePenalty get(String key) {
        return REG.get(key);
    }

    public static void clearForTest() {
        REG.clear();
    }

    private ForgeDevicePenaltyRegistry() {}
}
