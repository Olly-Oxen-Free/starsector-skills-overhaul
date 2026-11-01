package skills_overhaul.effect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import skills_overhaul.effect.handlers.defense.SODefenseConfigHandler;
import skills_overhaul.effect.handlers.defense.SODefenseDerelictFortHandler;
import skills_overhaul.effect.handlers.defense.SODefenseFortressShieldHandler;
import skills_overhaul.effect.handlers.defense.SODefenseRedundantBaysHandler;
import skills_overhaul.effect.handlers.defense.SODefenseShieldMasteryHandler;
import skills_overhaul.effect.handlers.gunnery.SOGunneryGlassCannonHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regression for review 2026-11-01-2306-effect B1: six pilot handlers only modified
 * ship stats when Global.getCombatEngine().getPlayerShip() was non-null, which is
 * never true at purchase or load (campaign), and SkillsOverhaulCombatPlugin only
 * dispatched SO_piloted_stat. Their stat parts must be reachable as ShipStatEffect
 * and the combat plugin must dispatch to it.
 */
public class PilotShipStatCombatPathTest {
    public static void main(String[] args) throws Exception {
        String id = "node";

        Map<String, Object> s = new HashMap<String, Object>();
        apply(new SOGunneryGlassCannonHandler(), stats(s), ship(), id);
        assert near(stat(s, "getBallisticWeaponDamageMult"), 1.30f) : "glass cannon ballistic damage";
        assert near(stat(s, "getEnergyWeaponDamageMult"), 1.30f) : "glass cannon energy damage";
        assert near(stat(s, "getMissileWeaponDamageMult"), 1.30f) : "glass cannon missile damage";
        assert near(stat(s, "getHullDamageTakenMult"), 1.20f) : "glass cannon hull damage taken";

        s = new HashMap<String, Object>();
        apply(new SODefenseRedundantBaysHandler(), stats(s), ship(), id);
        assert near(stat(s, "getFighterRefitTimeMult"), 0.85f) : "redundant bays refit time";

        s = new HashMap<String, Object>();
        apply(new SODefenseConfigHandler(), stats(s), ship("shield_shunt"), id);
        assert near(stat(s, "getFluxDissipation"), 1.08f) : "defensive config flux dissipation";
        assert near(bonus(s, "getArmorBonus", 100f), 112f) : "defensive config armor";
        assert near(stat(s, "getEmpDamageTakenMult"), 0.75f) : "defensive config emp taken";
        assert near(bonus(s, "getWeaponTurnRateBonus", 100f), 133f) : "defensive config turn rate";

        s = new HashMap<String, Object>();
        apply(new SODefenseConfigHandler(), stats(s), ship(), id);
        assert !s.containsKey("getFluxDissipation") : "defensive config must need shield_shunt";

        s = new HashMap<String, Object>();
        apply(new SODefenseDerelictFortHandler(), stats(s), ship(), id);
        assert !s.containsKey("getArmorBonus") : "derelict fort must need d-mods";

        s = new HashMap<String, Object>();
        apply(new SODefenseFortressShieldHandler(), stats(s), ship(), id);
        assert near(stat(s, "getShieldAbsorptionMult"), 0.90f) : "fortress shield absorption";
        assert near(bonus(s, "getShieldArcBonus", 90f), 110f) : "fortress shield arc";

        s = new HashMap<String, Object>();
        apply(new SODefenseShieldMasteryHandler(), stats(s), ship(), id);
        assert near(stat(s, "getShieldUpkeepMult"), 0.75f) : "shield mastery upkeep";

        String plugin = read("src/skills_overhaul/effect/SkillsOverhaulCombatPlugin.java");
        assert plugin.contains("instanceof ShipStatEffect")
                : "SkillsOverhaulCombatPlugin does not dispatch ShipStatEffect handlers";

        String registry = read("src/skills_overhaul/effect/EffectRegistry.java");
        for (String[] e : new String[][] {
                {"SO_gunnery_glass_cannon", "SOGunneryGlassCannonHandler"},
                {"SO_defense_redundant_bays", "SODefenseRedundantBaysHandler"},
                {"SO_defense_config", "SODefenseConfigHandler"},
                {"SO_defense_derelict_fort", "SODefenseDerelictFortHandler"},
                {"SO_defense_fortress_shield", "SODefenseFortressShieldHandler"},
                {"SO_defense_shield_mastery", "SODefenseShieldMasteryHandler"}}) {
            assert registry.matches("(?s).*\"" + e[0] + "\",\\s*new [\\w.]*\\." + e[1] + "\\(\\).*")
                    : e[0] + " is not registered to " + e[1];
        }

        System.out.println("ALL PASSED");
    }

    static void apply(EffectHandler h, MutableShipStatsAPI s, ShipAPI ship, String id) {
        assert h instanceof ShipStatEffect : h.getClass().getSimpleName() + " has no combat stat path";
        ((ShipStatEffect) h).applyTo(s, ship, id, null);
    }

    static float stat(Map<String, Object> s, String getter) {
        Object o = s.get(getter);
        assert o != null : getter + " was not touched";
        return ((MutableStat) o).getModifiedValue();
    }

    static float bonus(Map<String, Object> s, String getter, float base) {
        Object o = s.get(getter);
        assert o != null : getter + " was not touched";
        return ((StatBonus) o).computeEffective(base);
    }

    static boolean near(float a, float b) { return Math.abs(a - b) < 1e-3f; }

    /** MutableShipStatsAPI whose stat getters hand out real MutableStat/StatBonus objects, recorded in {@code touched}. */
    static MutableShipStatsAPI stats(final Map<String, Object> touched) {
        return (MutableShipStatsAPI) Proxy.newProxyInstance(
                MutableShipStatsAPI.class.getClassLoader(), new Class<?>[] {MutableShipStatsAPI.class},
                new InvocationHandler() {
                    public Object invoke(Object p, java.lang.reflect.Method m, Object[] a) {
                        Class<?> r = m.getReturnType();
                        if (r == MutableStat.class || r == StatBonus.class) {
                            Object o = touched.get(m.getName());
                            if (o == null) {
                                o = r == MutableStat.class ? new MutableStat(1f) : new StatBonus();
                                touched.put(m.getName(), o);
                            }
                            return o;
                        }
                        return defaultFor(r);
                    }
                });
    }

    static ShipAPI ship(String... hullMods) {
        final Set<String> mods = new HashSet<String>(Arrays.asList(hullMods));
        final ShipVariantAPI variant = (ShipVariantAPI) Proxy.newProxyInstance(
                ShipVariantAPI.class.getClassLoader(), new Class<?>[] {ShipVariantAPI.class},
                new InvocationHandler() {
                    public Object invoke(Object p, java.lang.reflect.Method m, Object[] a) {
                        if (m.getName().equals("hasHullMod")) return mods.contains(a[0]);
                        if (m.getName().equals("getHullMods")) {
                            Class<?> r = m.getReturnType();
                            if (r.isAssignableFrom(ArrayList.class)) return new ArrayList<String>(mods);
                            return new java.util.LinkedHashSet<String>(mods);
                        }
                        return defaultFor(m.getReturnType());
                    }
                });
        return (ShipAPI) Proxy.newProxyInstance(
                ShipAPI.class.getClassLoader(), new Class<?>[] {ShipAPI.class},
                new InvocationHandler() {
                    public Object invoke(Object p, java.lang.reflect.Method m, Object[] a) {
                        if (m.getName().equals("getVariant")) return variant;
                        return defaultFor(m.getReturnType());
                    }
                });
    }

    static Object defaultFor(Class<?> r) {
        if (r == boolean.class) return false;
        if (r == int.class) return 0;
        if (r == float.class) return 0f;
        if (r == long.class) return 0L;
        if (r == double.class) return 0d;
        if (r == List.class || r == Collection.class) return new ArrayList<Object>();
        if (r == Set.class) return new HashSet<Object>();
        return null;
    }

    /** Tests run from jars/classes (see build.sh); walk up to the mod root. */
    static String read(String rel) throws Exception {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            Path p = dir.resolve(rel);
            if (Files.exists(p)) return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot find " + rel);
    }
}
