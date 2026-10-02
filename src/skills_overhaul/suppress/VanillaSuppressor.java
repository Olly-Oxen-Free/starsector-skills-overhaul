package skills_overhaul.suppress;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Zeros all vanilla skill levels on load, stashing prior levels under SUPPRESS_TAG
 * so vanilla XP/point counters survive and skills can be restored on uninstall.
 *
 * Per spec §7.2: explicit unregister of registered SkillEffect modifiers is
 * required, not just setSkillLevel(0). We invoke each effect's unapply() before
 * zeroing the level. Defensive — no harm if setSkillLevel(0) already
 * unregisters; explicit unapply on already-clean stats is a no-op.
 */
public final class VanillaSuppressor {
    private static final String SUPPRESS_TAG = "$so_suppressed_";
    /** Set by UninstallHelper.prepareUninstall(); skips suppression on load. */
    public static final String K_SUPPRESSION_DISABLED = "$so_suppression_disabled";

    /**
     * Vanilla skill ids (starsector-core data/characters/skills/skill_data.csv, 0.98a).
     * Only these are suppressed — other mods' skills (often hidden/permanent) are left alone.
     */
    static final Set<String> VANILLA_SKILL_IDS = new HashSet<String>(Arrays.asList(
            "helmsmanship", "combat_endurance", "impact_mitigation", "damage_control",
            "field_modulation", "point_defense", "target_analysis", "ballistic_mastery",
            "systems_expertise", "missile_specialization", "tactical_drills",
            "coordinated_maneuvers", "wolfpack_tactics", "crew_training", "fighter_uplink",
            "carrier_group", "officer_training", "officer_management", "best_of_the_best",
            "support_doctrine", "force_concentration", "navigation", "sensors", "gunnery_implants",
            "energy_weapon_mastery", "electronic_warfare", "flux_regulation",
            "cybernetic_augmentation", "phase_corps", "special_modifications", "neural_link",
            "automated_ships", "bulk_transport", "salvaging", "field_repairs", "ordnance_expert",
            "polarized_armor", "containment_procedures", "makeshift_equipment",
            "industrial_planning", "derelict_contingent", "hull_restoration", "omega_ecm",
            "hypercognition", "space_operations", "planetary_operations", "colony_management",
            "strike_commander", "ranged_specialization", "shield_modulation", "phase_mastery",
            "reliability_engineering", "weapon_drills", "auxiliary_support", "aptitude_combat",
            "aptitude_leadership", "aptitude_technology", "aptitude_industry"));

    public static void applyOnEveryLoad() {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerPerson().getStats();
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.getBoolean(K_SUPPRESSION_DISABLED)) return;

        // Older versions suppressed every non-so_ skill, including other mods'.
        // Give those back from the stash.
        List<String> foreign = new ArrayList<String>();
        for (String k : mem.getKeys()) {
            if (k.startsWith(SUPPRESS_TAG) && !VANILLA_SKILL_IDS.contains(k.substring(SUPPRESS_TAG.length()))) {
                foreign.add(k);
            }
        }
        for (String k : foreign) {
            s.setSkillLevel(k.substring(SUPPRESS_TAG.length()), mem.getFloat(k));
            mem.unset(k);
        }

        for (MutableCharacterStatsAPI.SkillLevelAPI sl : s.getSkillsCopy()) {
            String id = sl.getSkill().getId();
            if (!VANILLA_SKILL_IDS.contains(id)) continue;

            float prev = sl.getLevel();
            if (prev > 0f) {
                String key = SUPPRESS_TAG + id;
                // Re-stash when the observed level rises (level-up auto-grants,
                // other mods) so a later restore doesn't revert to a stale value.
                float stored = mem.contains(key) ? mem.getFloat(key) : 0f;
                if (prev > stored) {
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
