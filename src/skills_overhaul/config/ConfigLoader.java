package skills_overhaul.config;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;

public final class ConfigLoader {
    private static Config instance;

    public static Config get() {
        if (instance == null) throw new IllegalStateException("Config not loaded");
        return instance;
    }

    public static void load() throws Exception {
        JSONObject j = Global.getSettings().getMergedJSONForMod(
                "data/config/skills_overhaul.json", "skills_overhaul");
        instance = Config.fromJSON(j);
    }
}
