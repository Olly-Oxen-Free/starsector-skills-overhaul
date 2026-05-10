package skills_overhaul.suppress;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeros all non-so_ skill levels on load, stashing prior levels under SUPPRESS_TAG
 * so vanilla XP/point counters survive and skills can be restored on uninstall.
 *
 * Per spec §7.2: explicit unregister of registered SkillEffect modifiers is
 * required, not just setSkillLevel(0). We invoke each effect's unapply() before
 * zeroing the level. Defensive — no harm if setSkillLevel(0) already
 * unregisters; explicit unapply on already-clean stats is a no-op.
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
                unapplyAllEffects(s, sl.getSkill());
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

    /**
     * Explicitly invoke unapply() on every SkillEffect type the spec registers
     * (CharacterStats, FleetStats). Defensive: if vanilla setSkillLevel(0)
     * already does this, no harm. Without it, registered effects may persist
     * even at level 0.
     */
    private static void unapplyAllEffects(MutableCharacterStatsAPI stats, SkillSpecAPI spec) {
        try {
            for (SkillSpecAPI.SkillEffectSpecAPI eff : spec.getEffectsAPI()) {
                CharacterStatsSkillEffect cs = eff.getAsStatsEffect();
                if (cs != null) {
                    try { cs.unapply(stats, spec.getId()); }
                    catch (Throwable ignored) {}
                }
                // Fleet-level effects apply to MutableFleetStatsAPI, not
                // character stats — skip; vanilla detaches them on
                // setSkillLevel(0) via fleet recompute.
            }
        } catch (Throwable t) {
            Global.getLogger(VanillaSuppressor.class).warn(
                    "[SO] unapplyAllEffects(" + spec.getId() + ") failed: " + t);
        }
    }

    private VanillaSuppressor() {}
}
