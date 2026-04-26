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
        skills_overhaul.corsair.PrivateerLifecycleScript privLifecycle =
                new skills_overhaul.corsair.PrivateerLifecycleScript();
        Global.getSector().addTransientScript(privLifecycle);
        Global.getSector().addTransientListener(privLifecycle);
        skills_overhaul.suppress.VanillaSuppressor.applyOnEveryLoad();
        skills_overhaul.migration.MigrationPlugin.runOnce(newGame);
        Global.getLogger(SkillsOverhaulModPlugin.class).info(
                LOG_PREFIX + "listeners registered; newGame=" + newGame);
    }
}
