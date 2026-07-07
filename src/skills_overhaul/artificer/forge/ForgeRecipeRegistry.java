package skills_overhaul.artificer.forge;

import java.util.*;

public final class ForgeRecipeRegistry {

    private static final Map<String, ForgeDeviceRecipe> ALL = new LinkedHashMap<>();
    private static final Set<String> UNLOCKED = new HashSet<>();

    public static void register(ForgeDeviceRecipe r) { ALL.put(r.id, r); }
    public static void unlock(String id) { if (ALL.containsKey(id)) UNLOCKED.add(id); }
    public static void lock(String id) { UNLOCKED.remove(id); }
    public static boolean isUnlocked(String id) { return UNLOCKED.contains(id); }
    public static ForgeDeviceRecipe get(String id) { return ALL.get(id); }

    public static Collection<ForgeDeviceRecipe> getAllUnlocked() {
        List<ForgeDeviceRecipe> out = new ArrayList<>();
        for (String id : UNLOCKED) {
            ForgeDeviceRecipe r = ALL.get(id);
            if (r != null) out.add(r);
        }
        return out;
    }

    public static Collection<ForgeDeviceRecipe> getAllUnlocked(ForgeDeviceType type) {
        List<ForgeDeviceRecipe> out = new ArrayList<>();
        for (ForgeDeviceRecipe r : getAllUnlocked()) {
            if (r.type == type) out.add(r);
        }
        return out;
    }

    /** Test helper. */
    public static void clearForTest() {
        ALL.clear();
        UNLOCKED.clear();
    }

    /** Load-time reset: unlocks are per-save; recipe definitions (ALL) are not. */
    public static void resetUnlocks() {
        UNLOCKED.clear();
    }

    /** Register all 19 spec'd recipes. Called once at mod load. */
    public static void registerAll() {
        register(new ForgeDeviceRecipe("drone_wakes", "Drone Wakes", ForgeDeviceType.BATTLE,
                5f, 0, "drone_wakes_penalty", "drone_wakes",
                "Flagship spawns PD drones at battle start"));
        register(new ForgeDeviceRecipe("sensor_beacon", "Sensor Beacon", ForgeDeviceType.CAMPAIGN,
                3f, 1, null, "sensor_beacon",
                "Drops a stationary sensor beacon"));
        register(new ForgeDeviceRecipe("ecm_wave", "ECM Wave Generator", ForgeDeviceType.BATTLE,
                4f, 0, "ecm_wave_penalty", "ecm_wave",
                "Battle-start ECM amplification + weapon-disable pulse"));
        register(new ForgeDeviceRecipe("comm_jammer", "Comm Jammer", ForgeDeviceType.CAMPAIGN,
                5f, 2, null, "comm_jammer",
                "Disrupts a faction's comms for 14 days"));
        register(new ForgeDeviceRecipe("self_replicating_minefield", "Self-Replicating Minefield",
                ForgeDeviceType.BATTLE, 8f, 0, "minefield_penalty", "self_replicating_minefield",
                "Spawns mines that respawn on death"));
        register(new ForgeDeviceRecipe("repair_pod", "Repair Pod", ForgeDeviceType.BATTLE,
                10f, 0, "repair_pod_penalty", "repair_pod",
                "Neutral objective with repair aura"));
        register(new ForgeDeviceRecipe("slipstream_seeder", "Slipstream Seeder", ForgeDeviceType.CAMPAIGN,
                8f, 5, null, "slipstream_seeder",
                "Spawns a slipstream segment"));
        register(new ForgeDeviceRecipe("nav_beacon_generator", "Nav Beacon Generator",
                ForgeDeviceType.CAMPAIGN, 4f, 3, null, "nav_beacon_generator",
                "Reduces burn cost in radius"));
        register(new ForgeDeviceRecipe("core_surge", "Core Surge", ForgeDeviceType.BATTLE,
                6f, 0, "core_surge_penalty", "core_surge",
                "Refunds system charges + activation damage buff"));
        register(new ForgeDeviceRecipe("officer_imprint", "Officer Imprint", ForgeDeviceType.CAMPAIGN,
                12f, 7, null, "officer_imprint",
                "Copies officer skills to fighter wing"));
        register(new ForgeDeviceRecipe("scrap_beacon", "Scrap Beacon", ForgeDeviceType.CAMPAIGN,
                5f, 3, null, "scrap_beacon",
                "Periodic supply income"));
        register(new ForgeDeviceRecipe("disruptor_relay", "Disruptor Relay", ForgeDeviceType.BATTLE,
                7f, 0, "disruptor_relay_penalty", "disruptor_relay",
                "EMP arcs from relay points"));
        register(new ForgeDeviceRecipe("graviton_anchor", "Graviton Anchor", ForgeDeviceType.BATTLE,
                8f, 0, "graviton_anchor_penalty", "graviton_anchor",
                "Slows enemies in radius"));
        register(new ForgeDeviceRecipe("derelict_drone_spawner", "Derelict Drone Spawner",
                ForgeDeviceType.BATTLE, 12f, 0, "derelict_drone_penalty", "derelict_drone_spawner",
                "Spawns combat drones"));
        register(new ForgeDeviceRecipe("transverse_vent", "Transverse Vent", ForgeDeviceType.BATTLE,
                10f, 0, "transverse_vent_penalty", "transverse_vent",
                "Fleet-wide instant flux vent"));
        register(new ForgeDeviceRecipe("hypercloud_generator", "Hypercloud Generator",
                ForgeDeviceType.CAMPAIGN, 12f, 7, null, "hypercloud_generator",
                "Hyperspace cloud reduces sensor profile"));
        register(new ForgeDeviceRecipe("pulsar_array", "Pulsar Array", ForgeDeviceType.BATTLE,
                15f, 0, "pulsar_array_penalty", "pulsar_array",
                "Pulsar beam across battlefield"));
        register(new ForgeDeviceRecipe("magnetic_field_emitter", "Magnetic Field Emitter",
                ForgeDeviceType.BATTLE, 12f, 0, "magnetic_field_penalty", "magnetic_field_emitter",
                "Localized magnetic field zone"));
        register(new ForgeDeviceRecipe("alpha_storm_emitter", "Alpha Storm Emitter",
                ForgeDeviceType.BATTLE, 20f, 0, "alpha_storm_emitter_penalty", "alpha_storm_emitter",
                "Localized Alpha Storm terrain"));
    }
}
