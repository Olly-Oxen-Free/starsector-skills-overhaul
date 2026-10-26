package skills_overhaul.config;

import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

public class ConfigTest {
    public static void main(String[] args) throws Exception {
        JSONObject j = new JSONObject()
                .put("points_per_level", 2)
                .put("migration_mult", 1.5)
                .put("good_standing_threshold", 5);
        Config cfg = Config.fromJSON(j);

        assert cfg.pointsPerLevel == 2 : "pointsPerLevel";
        assert Math.abs(cfg.migrationMult - 1.5) < 1e-6 : "migrationMult";
        assert cfg.goodStandingThreshold == 5 : "threshold";
        // default fallback
        assert Math.abs(cfg.autopilotXpMult - 0.80) < 1e-6 : "autopilot default";
        assert cfg.uiHotkey == Keyboard.KEY_K : "ui_hotkey default";
        assert Keyboard.KEY_K == 37 : "KEY_K code matches skills_overhaul.json";

        Config over = Config.fromJSON(new JSONObject().put("ui_hotkey", Keyboard.KEY_L));
        assert over.uiHotkey == Keyboard.KEY_L : "ui_hotkey override";

        System.out.println("ALL PASSED");
    }
}
