package skills_overhaul.suppress;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeros all non-so_ skill levels on load, stashing prior levels under SUPPRESS_TAG
 * so vanilla XP/point counters survive and skills can be restored on uninstall.
 *
 * FIXME(playtest): verify setSkillLevel(id, 0f) actually unregisters the vanilla
 * skill's stat mods on an existing save — if vanilla caches bonuses elsewhere,
 * may need additional teardown path.
 */
public final class VanillaSuppressor {
    private static final String SUPPRESS_TAG = "$so_suppressed_";

    public static void applyOnEveryLoad() {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerPerson().getStats();
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        for (MutableCharacterStatsAPI.SkillLevelAPI sl : s.getSkillsCopy()) {
            String id = sl.getSkill().getId();
            if (id.startsWith("so_")) continue;

            float prev = sl.getLevel();
            if (prev > 0f) {
                String key = SUPPRESS_TAG + id;
                if (!mem.contains(key)) {
                    mem.set(key, prev);
                }
                s.setSkillLevel(id, 0f);
            }
        }
    }

    /** Uninstall path — restore vanilla skill levels from stash. */
    public static void restoreAll() {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerPerson().getStats();
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        List<String> toClear = new ArrayList<String>();
        for (String k : mem.getKeys()) {
            if (!k.startsWith(SUPPRESS_TAG)) continue;
            String id = k.substring(SUPPRESS_TAG.length());
            float prev = mem.getFloat(k);
            s.setSkillLevel(id, prev);
            toClear.add(k);
        }
        for (String k : toClear) mem.unset(k);
    }
}
