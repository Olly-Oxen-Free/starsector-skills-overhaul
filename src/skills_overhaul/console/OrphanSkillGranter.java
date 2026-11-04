package skills_overhaul.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;
import skills_overhaul.effect.EffectRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Orphan skill access for nodes spec'd but absent from the tree.
 *
 * Spec lists Impact Mitigation (T1) and Carrier Supremacy (T4) but the
 * 5-slot/tier grid is full for both rows. Per design, these remain
 * implemented but off-tree; players grant them via Console Commands:
 *
 *   runcode skills_overhaul.console.OrphanSkillGranter.grant("impact_mitigation");
 *   runcode skills_overhaul.console.OrphanSkillGranter.grant("carrier_supremacy");
 *   runcode skills_overhaul.console.OrphanSkillGranter.revoke("impact_mitigation");
 *
 * State stored in MemoryAPI key "$so_orphan_<name>" so respec / save-reload
 * preserves the grant. Re-applies on game load via SkillsOverhaulModPlugin.
 */
public final class OrphanSkillGranter {

    private static final String KEY_PREFIX = "$so_orphan_";

    public static void grant(String name) {
        OrphanSpec spec = lookup(name);
        if (spec == null) { System.out.println("Unknown orphan skill: " + name); return; }

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.set(KEY_PREFIX + name, true);
        applyHandler(spec);
        System.out.println("Granted: " + name);
    }

    public static void revoke(String name) {
        OrphanSpec spec = lookup(name);
        if (spec == null) { System.out.println("Unknown orphan skill: " + name); return; }

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.unset(KEY_PREFIX + name);
        EffectHandler h = EffectRegistry.get(spec.effectType);
        if (h != null) h.remove(spec.nodeId, spec.params);
        System.out.println("Revoked: " + name);
    }

    /** Called from SkillsOverhaulModPlugin.onGameLoad to re-apply persistent grants. */
    public static void applyAllOnLoad() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        for (OrphanSpec s : ORPHANS) {
            if (mem.contains(KEY_PREFIX + s.name) && mem.getBoolean(KEY_PREFIX + s.name)) {
                applyHandler(s);
            }
        }
    }

    /**
     * Granted orphans, for SkillsOverhaulCombatPlugin. Their effects are ship
     * stats, which are rebuilt every battle, so they must be applied per combat
     * like tree nodes; the load-time apply cannot reach the flagship.
     */
    public static List<OrphanSpec> granted(Predicate<String> isSet) {
        List<OrphanSpec> out = new ArrayList<OrphanSpec>();
        for (OrphanSpec s : ORPHANS) if (isSet.test(KEY_PREFIX + s.name)) out.add(s);
        return out;
    }

    private static void applyHandler(OrphanSpec s) {
        EffectHandler h = EffectRegistry.get(s.effectType);
        if (h != null) h.apply(s.nodeId, s.params);
        else System.out.println("No handler registered for " + s.effectType);
    }

    private static OrphanSpec lookup(String name) {
        for (OrphanSpec s : ORPHANS) if (s.name.equals(name)) return s;
        return null;
    }

    private static final OrphanSpec[] ORPHANS = new OrphanSpec[] {
            new OrphanSpec(
                    "impact_mitigation",
                    "so_defense_impact_mitigation",
                    "SO_piloted_stat",
                    buildJSON("hitChance", 0.50, "damageReduction", 0.25)),
            new OrphanSpec(
                    "carrier_supremacy",
                    "so_defense_carrier_supremacy",
                    "SO_defense_carrier_supremacy",
                    new JSONObject())
    };

    private static JSONObject buildJSON(Object... kv) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i + 1 < kv.length; i += 2) o.put((String) kv[i], kv[i + 1]);
        } catch (Exception e) { /* ignore */ }
        return o;
    }

    public static final class OrphanSpec {
        public final String name;
        public final String nodeId;
        public final String effectType;
        public final JSONObject params;
        OrphanSpec(String name, String nodeId, String effectType, JSONObject params) {
            this.name = name; this.nodeId = nodeId;
            this.effectType = effectType; this.params = params;
        }
    }

    private OrphanSkillGranter() {}

    // === Admin XP crisis trigger (A4) ===
    // Spec §5.3(e) — colony crises are internal API; no public ColonyCrisis
    // event surface. Players or future mod integrations can fire crisis-
    // resolved events manually via console:
    //   runcode skills_overhaul.console.OrphanSkillGranter.fireCrisis("luddic_path_cell", marketId);
    public static void fireCrisis(String crisisType, String marketId) {
        com.fs.starfarer.api.campaign.econ.MarketAPI m =
                com.fs.starfarer.api.Global.getSector().getEconomy().getMarket(marketId);
        if (m == null) {
            System.out.println("Unknown market id: " + marketId);
            return;
        }
        new skills_overhaul.xp.AdminXPTracker().onCrisisResolved(crisisType, m);
        System.out.println("Crisis fired: " + crisisType + " on " + marketId);
    }
}
