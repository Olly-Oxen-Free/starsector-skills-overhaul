package skills_overhaul.effect;

import org.json.JSONObject;
import skills_overhaul.effect.handlers.corsair.SalvageMultiplierHandler;
import skills_overhaul.effect.handlers.corsair.StatBonusHandler;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {
    private static final Map<String, EffectHandler> handlers = new HashMap<>();

    static {
        handlers.put("SO_corsair_stat_bonus",      new StatBonusHandler());
        handlers.put("SO_corsair_salvage_mult",    new SalvageMultiplierHandler());
        handlers.put("SO_corsair_combat_script",
                new skills_overhaul.effect.handlers.corsair.CombatScriptHandler());
        handlers.put("SO_corsair_bounty_board",
                new skills_overhaul.effect.handlers.corsair.BountyBoardHandler());
        handlers.put("SO_corsair_dmod_dp",
                new skills_overhaul.effect.handlers.corsair.DModDpScalingHandler());
        handlers.put("SO_corsair_cap_mult",
                new skills_overhaul.effect.handlers.corsair.CapMultiplierHandler());
        handlers.put("SO_corsair_t0",
                new skills_overhaul.effect.handlers.corsair.CorsairT0Handler());
        handlers.put("SO_corsair_letter_of_marque",
                new skills_overhaul.effect.handlers.corsair.LetterOfMarqueHandler());
        handlers.put("SO_fleet_stat", new skills_overhaul.effect.handlers.SOFleetStatHandler());
        handlers.put("SO_combat_script", new skills_overhaul.effect.handlers.SOCombatScriptHandler());
        handlers.put("SO_commandant_t0", new skills_overhaul.effect.handlers.SOCommandantT0Handler());
        handlers.put("SO_command_field_mod", new skills_overhaul.effect.handlers.SOCommandFieldModHandler());
        handlers.put("SO_objective_scaling", new skills_overhaul.effect.handlers.SOObjectiveScalingHandler());
        handlers.put("SO_commandant_t5", new skills_overhaul.effect.handlers.SOCommandantT5Handler());
        handlers.put("SO_refitting", new skills_overhaul.effect.handlers.SORefittingHandler());
        handlers.put("SO_objective_capture_buff", new skills_overhaul.effect.handlers.SOObjectiveCaptureBuffHandler());
        handlers.put("SO_doctrine_hierarchy", new skills_overhaul.effect.handlers.SODoctrineHierarchyHandler());
        handlers.put("SO_officer_inspiration", new skills_overhaul.effect.handlers.SOOfficerInspirationHandler());
        handlers.put("SO_field_commands", new skills_overhaul.effect.handlers.SOFieldCommandsHandler());
        handlers.put("SO_elite_cascade", new skills_overhaul.effect.handlers.SOEliteCascadeHandler());
        handlers.put("SO_zeal_combat_script", new skills_overhaul.effect.handlers.SOZealCombatScriptHandler());
        handlers.put("SO_zealot_t0", new skills_overhaul.effect.handlers.SOZealotT0Handler());
        handlers.put("SO_zealot_t5", new skills_overhaul.effect.handlers.SOZealotT5Handler());
        handlers.put("SO_mobile_arsenal", new skills_overhaul.effect.handlers.SOMobileArsenalHandler());
        handlers.put("SO_field_logistics", new skills_overhaul.effect.handlers.SOFieldLogisticsHandler());
        handlers.put("SO_forced_march", new skills_overhaul.effect.handlers.SOForcedMarchHandler());
        handlers.put("SO_heavy_detachments", new skills_overhaul.effect.handlers.SOHeavyDetachmentsHandler());
        handlers.put("SO_doctrinal_authority", new skills_overhaul.effect.handlers.SODoctrinalAuthorityHandler());
        handlers.put("SO_artificer_t0", new skills_overhaul.effect.handlers.SOArtificerT0Handler());
        handlers.put("SO_artificer_t5", new skills_overhaul.effect.handlers.SOArtificerT5Handler());
        handlers.put("SO_forge_recipe_unlock", new skills_overhaul.effect.handlers.SOForgeRecipeUnlockHandler());
        handlers.put("SO_forge_quota_bonus", new skills_overhaul.effect.handlers.SOForgeQuotaBonusHandler());
        handlers.put("SO_tech_cascade_stub", new skills_overhaul.effect.handlers.SOTechCascadeStubHandler());
        handlers.put("SO_artificer_combat_script", new skills_overhaul.effect.handlers.SOArtificerCombatScriptHandler());
        handlers.put("SO_automated_points_bonus", new skills_overhaul.effect.handlers.SOAutomatedPointsBonusHandler());
        handlers.put("SO_phase_shared_bonus", new skills_overhaul.effect.handlers.SOPhaseSharedBonusHandler());
        handlers.put("SO_carrier_bay_bonus", new skills_overhaul.effect.handlers.SOCarrierBayBonusHandler());
        handlers.put("SO_final_gambit", new skills_overhaul.effect.handlers.SOFinalGambitHandler());
        handlers.put("SO_stealth_systems", new skills_overhaul.effect.handlers.SOStealthSystemsHandler());
        handlers.put("SO_nav_matrix", new skills_overhaul.effect.handlers.SONavMatrixHandler());

        // ── Pilot — shared stat handler ────────────────────────────────────────
        handlers.put("SO_piloted_stat", new skills_overhaul.effect.handlers.SOPilotedStatHandler());

        // ── Pilot — Defense lane ───────────────────────────────────────────────
        {
            skills_overhaul.effect.handlers.defense.SODefenseCombatScriptHandler defScript =
                    new skills_overhaul.effect.handlers.defense.SODefenseCombatScriptHandler();
            // Plugin-activation markers (all → ACTIVE_NODES only)
            handlers.put("SO_defense_fortify",           defScript);
            handlers.put("SO_defense_targe",             defScript);
            handlers.put("SO_defense_deaths_door",       defScript);
            handlers.put("SO_defense_adamantine",        defScript);
            handlers.put("SO_defense_iron_bastion",      defScript);
            handlers.put("SO_defense_lead_front",        defScript);
            handlers.put("SO_defense_unbreakable",
                    new skills_overhaul.effect.handlers.defense.SODefenseUnbreakableHandler());
            handlers.put("SO_defense_flux_fortress",     defScript);
            handlers.put("SO_defense_overwhelming_force",defScript);
            // Stat-applying handlers (also add to ACTIVE_NODES)
            handlers.put("SO_defense_fortress_shield",
                    new skills_overhaul.effect.handlers.defense.SODefenseFortressShieldHandler());
            handlers.put("SO_defense_redundant_bays",
                    new skills_overhaul.effect.handlers.defense.SODefenseRedundantBaysHandler());
            handlers.put("SO_defense_config",
                    new skills_overhaul.effect.handlers.defense.SODefenseConfigHandler());
            handlers.put("SO_defense_shield_mastery",
                    new skills_overhaul.effect.handlers.defense.SODefenseShieldMasteryHandler());
            handlers.put("SO_defense_derelict_fort",
                    new skills_overhaul.effect.handlers.defense.SODefenseDerelictFortHandler());
            handlers.put("SO_defense_carrier_supremacy",
                    new skills_overhaul.effect.handlers.defense.SODefenseCarrierSupremacyHandler());
        }

        // ── Pilot — Gunnery lane ───────────────────────────────────────────────
        {
            skills_overhaul.effect.handlers.gunnery.SOGunneryCombatScriptHandler gunScript =
                    new skills_overhaul.effect.handlers.gunnery.SOGunneryCombatScriptHandler();
            handlers.put("SO_gunnery_lock_on",           gunScript);
            handlers.put("SO_gunnery_overload_hunter",   gunScript);
            handlers.put("SO_gunnery_threat_acq",        gunScript);
            handlers.put("SO_gunnery_berserker",         gunScript);
            handlers.put("SO_gunnery_surgical",          gunScript);
            handlers.put("SO_gunnery_twist_knife",       gunScript);
            handlers.put("SO_gunnery_dragon_slayer",     gunScript);
            handlers.put("SO_gunnery_point_blank",       gunScript);
            handlers.put("SO_gunnery_bounty_hunter",     gunScript);
            handlers.put("SO_gunnery_wolfpack",          gunScript);
            handlers.put("SO_gunnery_weapons_discipline",gunScript);
            handlers.put("SO_gunnery_dead_eye",          gunScript);
            handlers.put("SO_gunnery_killing_machine",   gunScript);
            handlers.put("SO_gunnery_glass_cannon",
                    new skills_overhaul.effect.handlers.gunnery.SOGunneryGlassCannonHandler());
        }

        handlers.put("SO_collective_t0",
                new skills_overhaul.effect.handlers.collective.SOCollectiveT0Handler());
        // T1
        handlers.put("SO_collective_magnetic_shielding",
                new skills_overhaul.effect.handlers.collective.SOMagneticShieldingHandler());
        handlers.put("SO_collective_precision_fire_control",
                new skills_overhaul.effect.handlers.collective.SOPrecisionFireControlHandler());
        handlers.put("SO_collective_combat_memory",
                new skills_overhaul.effect.handlers.collective.SOCombatMemoryHandler());
        handlers.put("SO_collective_voltaic_conduits",
                new skills_overhaul.effect.handlers.collective.SOVoltaicConduitsHandler());
        handlers.put("SO_collective_integrated_logistics",
                new skills_overhaul.effect.handlers.collective.SOIntegratedLogisticsHandler());
        // T2
        handlers.put("SO_collective_deadly_persistence",
                new skills_overhaul.effect.handlers.collective.SODeadlyPersistenceHandler());
        handlers.put("SO_collective_battle_conditioning",
                new skills_overhaul.effect.handlers.collective.SOBattleConditioningHandler());
        handlers.put("SO_collective_shield_burst",
                new skills_overhaul.effect.handlers.collective.SOShieldBurstHandler());
        handlers.put("SO_collective_structural_integrity",
                new skills_overhaul.effect.handlers.collective.SOStructuralIntegrityHandler());
        handlers.put("SO_collective_hardened_targeting",
                new skills_overhaul.effect.handlers.collective.SOHardenedTargetingHandler());
        // T3
        handlers.put("SO_collective_mind_cascade",
                new skills_overhaul.effect.handlers.collective.SOMindCascadeHandler());
        handlers.put("SO_collective_wide_range_propagation",
                new skills_overhaul.effect.handlers.collective.SOWideRangePropagationHandler());
        handlers.put("SO_collective_first_strike",
                new skills_overhaul.effect.handlers.collective.SOFirstStrikeHandler());
        handlers.put("SO_collective_critical_state",
                new skills_overhaul.effect.handlers.collective.SOCriticalStateHandler());
        handlers.put("SO_collective_furor_machinae",
                new skills_overhaul.effect.handlers.collective.SOFurorMachinaeHandler());
        // T4
        handlers.put("SO_collective_emergency_heatsink",
                new skills_overhaul.effect.handlers.collective.SOEmergencyHeatsinkHandler());
        handlers.put("SO_collective_command_hierarchy",
                new skills_overhaul.effect.handlers.collective.SOCommandHierarchyHandler());
        handlers.put("SO_collective_lattice_defense",
                new skills_overhaul.effect.handlers.collective.SOLatticeDefenseHandler());
        handlers.put("SO_collective_termination_protocol",
                new skills_overhaul.effect.handlers.collective.SOTerminationProtocolHandler());
        handlers.put("SO_collective_voltaic_chain",
                new skills_overhaul.effect.handlers.collective.SOVoltaicChainHandler());
        // T5 (Capstone)
        handlers.put("SO_collective_transspatial_mind",
                new skills_overhaul.effect.handlers.collective.SOTranssspatialMindHandler());

        // ── Pilot — Systems lane ──────────────────────────────────────────────
        {
            skills_overhaul.effect.handlers.systems.SOSystemsCombatScriptHandler sysScript =
                    new skills_overhaul.effect.handlers.systems.SOSystemsCombatScriptHandler();
            // Plugin-activation no-op markers
            handlers.put("SO_systems_phase_coil",              sysScript);
            handlers.put("SO_systems_flux_exploitation",       sysScript);
            handlers.put("SO_systems_strike_coord",            sysScript);
            handlers.put("SO_systems_target_painter",          sysScript);
            handlers.put("SO_systems_kill_sync",               sysScript);
            handlers.put("SO_systems_reactive_systems",        sysScript);
            handlers.put("SO_systems_red_line",                sysScript);
            handlers.put("SO_systems_void_strike",             sysScript);
            handlers.put("SO_systems_phase_mastery",           sysScript);
            handlers.put("SO_systems_precision_strike",        sysScript);
            handlers.put("SO_systems_carrier_command",         sysScript);
            handlers.put("SO_systems_phase_resonance_merge",   sysScript);
            // Flat-stat + memory handlers
            handlers.put("SO_systems_emp_specialist",
                    new skills_overhaul.effect.handlers.systems.SOSystemsEMPSpecialistHandler());
            handlers.put("SO_systems_automata_integra",
                    new skills_overhaul.effect.handlers.systems.SOSystemsAutomataIntegraHandler());
            handlers.put("SO_systems_synaptic_relay",
                    new skills_overhaul.effect.handlers.systems.SOSystemsSynapticRelayHandler());
            handlers.put("SO_systems_experimental_overrides",
                    new skills_overhaul.effect.handlers.systems.SOSystemsExperimentalOverridesHandler());
        }

        // ── Pilot — Maneuvering lane ───────────────────────────────────────────
        {
            skills_overhaul.effect.handlers.maneuvering.SOManeuverCombatScriptHandler manScript =
                    new skills_overhaul.effect.handlers.maneuvering.SOManeuverCombatScriptHandler();
            handlers.put("SO_maneuver_stealth_adapt",   manScript);
            handlers.put("SO_maneuver_vent_sprint",     manScript);
            handlers.put("SO_maneuver_deep_vent",       manScript);
            handlers.put("SO_maneuver_knife_fighter",   manScript);
            handlers.put("SO_maneuver_reactive_drive",  manScript);
            handlers.put("SO_maneuver_phase_dancer",    manScript);
            handlers.put("SO_maneuver_crossfire",       manScript);
            handlers.put("SO_maneuver_zfm",             manScript);
            handlers.put("SO_maneuver_kinetic_cooling", manScript);
            handlers.put("SO_maneuver_ace",             manScript);
            handlers.put("SO_maneuver_ghost_protocol",  manScript);
            handlers.put("SO_maneuver_perfect_timing",  manScript);
            handlers.put("SO_maneuver_battle_focus",    manScript);
            handlers.put("SO_maneuver_flanking",        manScript);
            handlers.put("SO_maneuver_tactical_break",  manScript);
            handlers.put("SO_maneuver_untouchable",     manScript);
        }
    }

    public static EffectHandler get(String type) {
        EffectHandler h = handlers.get(type);
        return h != null ? h : NoOpHandler.INSTANCE;
    }
}

class NoOpHandler implements EffectHandler {
    static final NoOpHandler INSTANCE = new NoOpHandler();
    @Override public void apply(String id, JSONObject p) {
        com.fs.starfarer.api.Global.getLogger(NoOpHandler.class)
                .warn("[SkillsOverhaul] unknown effect type on " + id + ": " + p.optString("type"));
    }
    @Override public void remove(String id, JSONObject p) {}
}
