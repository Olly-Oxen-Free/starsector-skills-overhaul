package skills_overhaul;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import skills_overhaul.config.ConfigLoader;
import skills_overhaul.tree.TreeRegistry;
import skills_overhaul.state.Tree;

public class SkillsOverhaulModPlugin extends BaseModPlugin {
    public static final String MOD_ID = "skills_overhaul";
    public static final String LOG_PREFIX = "[SkillsOverhaul] ";

    @Override
    public void onApplicationLoad() throws Exception {
        ConfigLoader.load();
        TreeRegistry.load();
        skills_overhaul.artificer.forge.ForgeRecipeRegistry.registerAll();
        // Register all 19 device effects + penalties
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("drone_wakes", new skills_overhaul.artificer.forge.devices.DroneWakesDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("sensor_beacon", new skills_overhaul.artificer.forge.devices.SensorBeaconDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("ecm_wave", new skills_overhaul.artificer.forge.devices.ECMWaveDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("comm_jammer", new skills_overhaul.artificer.forge.devices.CommJammerDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("self_replicating_minefield", new skills_overhaul.artificer.forge.devices.SelfReplicatingMinefieldDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("repair_pod", new skills_overhaul.artificer.forge.devices.RepairPodDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("slipstream_seeder", new skills_overhaul.artificer.forge.devices.SlipstreamSeederDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("nav_beacon_generator", new skills_overhaul.artificer.forge.devices.NavBeaconGeneratorDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("core_surge", new skills_overhaul.artificer.forge.devices.CoreSurgeDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("officer_imprint", new skills_overhaul.artificer.forge.devices.OfficerImprintDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("scrap_beacon", new skills_overhaul.artificer.forge.devices.ScrapBeaconDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("disruptor_relay", new skills_overhaul.artificer.forge.devices.DisruptorRelayDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("graviton_anchor", new skills_overhaul.artificer.forge.devices.GravitonAnchorDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("derelict_drone_spawner", new skills_overhaul.artificer.forge.devices.DerelictDroneSpawnerDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("transverse_vent", new skills_overhaul.artificer.forge.devices.TransverseVentDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("hypercloud_generator", new skills_overhaul.artificer.forge.devices.HypercloudGeneratorDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("pulsar_array", new skills_overhaul.artificer.forge.devices.PulsarArrayDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("magnetic_field_emitter", new skills_overhaul.artificer.forge.devices.MagneticFieldEmitterDevice());
        skills_overhaul.artificer.forge.ForgeDeviceEffectRegistry.register("alpha_storm_emitter", new skills_overhaul.artificer.forge.devices.AlphaStormEmitterDevice());

        // Penalties (battle devices only; campaign devices have null penaltyKey)
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("drone_wakes_penalty", new skills_overhaul.artificer.forge.devices.DroneWakesPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("ecm_wave_penalty", new skills_overhaul.artificer.forge.devices.ECMWavePenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("minefield_penalty", new skills_overhaul.artificer.forge.devices.SelfReplicatingMinefieldPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("repair_pod_penalty", new skills_overhaul.artificer.forge.devices.RepairPodPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("core_surge_penalty", new skills_overhaul.artificer.forge.devices.CoreSurgePenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("disruptor_relay_penalty", new skills_overhaul.artificer.forge.devices.DisruptorRelayPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("graviton_anchor_penalty", new skills_overhaul.artificer.forge.devices.GravitonAnchorPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("derelict_drone_penalty", new skills_overhaul.artificer.forge.devices.DerelictDroneSpawnerPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("transverse_vent_penalty", new skills_overhaul.artificer.forge.devices.TransverseVentPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("pulsar_array_penalty", new skills_overhaul.artificer.forge.devices.PulsarArrayPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("magnetic_field_penalty", new skills_overhaul.artificer.forge.devices.MagneticFieldEmitterPenalty());
        skills_overhaul.artificer.forge.ForgeDevicePenaltyRegistry.register("alpha_storm_emitter_penalty", new skills_overhaul.artificer.forge.devices.AlphaStormEmitterPenalty());
        Global.getLogger(SkillsOverhaulModPlugin.class).info(LOG_PREFIX
                + "loaded " + TreeRegistry.totalNodeCount(Tree.PILOT) + " pilot nodes ("
                + TreeRegistry.getTrees(Tree.PILOT).size() + " trees), "
                + TreeRegistry.totalNodeCount(Tree.COMMANDER) + " cmd ("
                + TreeRegistry.getTrees(Tree.COMMANDER).size() + " trees), "
                + TreeRegistry.totalNodeCount(Tree.ADMIN) + " admin ("
                + TreeRegistry.getTrees(Tree.ADMIN).size() + " trees)");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientListener(new skills_overhaul.xp.CommanderXPListener());
        Global.getSector().getListenerManager().addListener(new skills_overhaul.xp.AdminXPTracker(), false);
        Global.getSector().addTransientScript(new skills_overhaul.level.LevelReconciler());
        Global.getSector().addTransientScript(new skills_overhaul.supply.SupplyBufferListener());
        Global.getSector().addTransientListener(new skills_overhaul.supply.SalvageBonusListener());
        Global.getSector().addTransientScript(new skills_overhaul.supply.SupplyBufferRenderer());
        Global.getSector().addTransientScript(new skills_overhaul.ui.CharacterTabReplacer());
        Global.getSector().addTransientScript(new skills_overhaul.corsair.InfamyDailyTicker());
        Global.getSector().addTransientListener(new skills_overhaul.corsair.InfamyKillListener());
        Global.getSector().addTransientListener(
                new skills_overhaul.effect.handlers.corsair.PostBattleSalvageMultiplierListener());
        Global.getSector().addTransientListener(
                new skills_overhaul.effect.handlers.corsair.BountyBoardListener());
        skills_overhaul.corsair.PrivateerLifecycleScript privLifecycle =
                new skills_overhaul.corsair.PrivateerLifecycleScript();
        Global.getSector().addTransientScript(privLifecycle);
        Global.getSector().addTransientListener(privLifecycle);
        Global.getSector().addTransientListener(new skills_overhaul.zealot.ZealKillListener());
        Global.getSector().addTransientScript(new skills_overhaul.zealot.ZealDailyTicker());
        Global.getSector().addTransientListener(new skills_overhaul.zealot.CrusadeNPCStub());
        Global.getSector().addTransientListener(new skills_overhaul.effect.handlers.PostBattleRestoreListener());
        Global.getSector().addTransientListener(new skills_overhaul.effect.handlers.RaidZealListener());
        Global.getSector().addTransientScript(new skills_overhaul.zealot.DoctrinalAuthorityTicker());
        Global.getSector().addTransientScript(
                new skills_overhaul.artificer.forge.MobileFabUnitTicker());
        Global.getSector().addTransientListener(
                new skills_overhaul.artificer.forge.ForgeBattleHook());
        skills_overhaul.suppress.VanillaSuppressor.applyOnEveryLoad();
        skills_overhaul.migration.MigrationPlugin.runOnce(newGame);
        Global.getLogger(SkillsOverhaulModPlugin.class).info(
                LOG_PREFIX + "listeners registered; newGame=" + newGame);
    }
}
