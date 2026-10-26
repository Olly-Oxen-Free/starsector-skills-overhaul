package skills_overhaul;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import skills_overhaul.barevents.SeasonedCrewBarEvent;
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
        // addListener(l, true) = transient. The old `false` registered a
        // persistent copy per load (saved into the save, stacking: N loads =
        // N monthly XP awards). removeListenerOfClass purges those from saves.
        Global.getSector().getListenerManager().removeListenerOfClass(skills_overhaul.xp.AdminXPTracker.class);
        Global.getSector().getListenerManager().addListener(new skills_overhaul.xp.AdminXPTracker(), true);
        Global.getSector().addTransientScript(new skills_overhaul.level.LevelReconciler());
        Global.getSector().addTransientListener(new skills_overhaul.supply.SalvageBonusListener());
        Global.getSector().addTransientScript(new skills_overhaul.supply.SupplyBufferRenderer());
        skills_overhaul.ui.host.HostRouter.reset();
        com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI lm = Global.getSector().getListenerManager();
        if (!lm.hasListenerOfClass(skills_overhaul.ui.host.CharTabHost.class)
                && !Global.getSector().hasTransientScript(skills_overhaul.ui.host.CharTabHost.class)) {
            // One instance is both the tab listener and the CHARACTER-tab poll script.
            skills_overhaul.ui.host.CharTabHost tabHost =
                    new skills_overhaul.ui.host.CharTabHost(new skills_overhaul.ui.host.HostRouter());
            lm.addListener(tabHost, true);
            Global.getSector().addTransientScript(tabHost);
        }
        if (!lm.hasListenerOfClass(skills_overhaul.ui.host.SkillsHotkeyListener.class)) {
            lm.addListener(new skills_overhaul.ui.host.SkillsHotkeyListener(), true);
        }
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
        Global.getSector().addTransientListener(new skills_overhaul.effect.handlers.PostBattleRestoreListener());
        Global.getSector().addTransientListener(new skills_overhaul.effect.handlers.RaidZealListener());
        Global.getSector().addTransientScript(new skills_overhaul.zealot.DoctrinalAuthorityTicker());
        Global.getSector().addTransientScript(
                new skills_overhaul.artificer.forge.MobileFabUnitTicker());
        Global.getSector().addTransientListener(
                new skills_overhaul.artificer.forge.ForgeBattleHook());
        Global.getSector().addTransientScript(
                new skills_overhaul.artificer.alphastorm.AlphaStormCampaignTicker());
        Global.getSector().addTransientListener(
                new skills_overhaul.collective.CollectiveBattleEndListener());
        Global.getSector().addTransientScript(
                new skills_overhaul.supply.ForgeCampaignBuffsTicker());
        // The creator is saved with BarEventManager; the old 0-day memory flag
        // expired, so a duplicate creator was added on every load.
        if (!BarEventManager.getInstance().hasEventCreator(SeasonedCrewBarEvent.class)) {
            BarEventManager.getInstance().addEventCreator(new SeasonedCrewBarEvent());
        }
        // Saves from those versions hold one creator per past load; keep the first.
        // getCreators() returns BarEventManager's live list.
        boolean keptSeasonedCrew = false;
        java.util.Iterator<BarEventManager.GenericBarEventCreator> creators =
                BarEventManager.getInstance().getCreators().iterator();
        while (creators.hasNext()) {
            if (!(creators.next() instanceof SeasonedCrewBarEvent)) continue;
            if (keptSeasonedCrew) creators.remove();
            keptSeasonedCrew = true;
        }
        // Migration must read vanilla skill levels BEFORE suppression zeroes
        // them, or every existing save migrates with spent=0.
        skills_overhaul.migration.MigrationPlugin.runOnce(newGame);
        skills_overhaul.suppress.VanillaSuppressor.applyOnEveryLoad();
        skills_overhaul.state.NodeApplyService.reapplyAllOwned();
        // After reapplyAllOwned: its static reset pass would otherwise wipe
        // the ACTIVE_NODES entries orphan grants just added.
        skills_overhaul.console.OrphanSkillGranter.applyAllOnLoad();
        Global.getLogger(SkillsOverhaulModPlugin.class).info(
                LOG_PREFIX + "listeners registered; newGame=" + newGame);
    }
}
