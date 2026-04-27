package skills_overhaul.artificer.forge;

import java.util.HashMap;
import java.util.Map;

public final class ForgeDeviceEffectRegistry {
    private static final Map<String, ForgeDeviceEffect> REG = new HashMap<>();

    public static void register(String key, ForgeDeviceEffect e) {
        REG.put(key, e);
    }

    public static ForgeDeviceEffect get(String key) {
        return REG.get(key);
    }

    public static void clearForTest() {
        REG.clear();
    }

    private ForgeDeviceEffectRegistry() {}
}
