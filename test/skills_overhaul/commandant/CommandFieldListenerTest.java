package skills_overhaul.commandant;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Pins which stats the Command Field aura modifies (reviews/2026-11-03-0435-commandant.md B1). */
public class CommandFieldListenerTest {
    public static void main(String[] args) throws Exception {
        final Set<String> touched = new HashSet<>();
        final Map<String, Object> statObjects = new HashMap<>();
        MutableShipStatsAPI stats = (MutableShipStatsAPI) Proxy.newProxyInstance(
                MutableShipStatsAPI.class.getClassLoader(),
                new Class<?>[]{MutableShipStatsAPI.class},
                (proxy, method, a) -> {
                    String name = method.getName();
                    touched.add(name);
                    Object stat = statObjects.get(name);
                    if (stat == null) {
                        Class<?> rt = method.getReturnType();
                        try {
                            stat = rt.getConstructor().newInstance();
                        } catch (NoSuchMethodException e) {
                            stat = rt.getConstructor(float.class).newInstance(1f);
                        }
                        statObjects.put(name, stat);
                    }
                    return stat;
                });

        CommandFieldListener.applyTotals(stats, new float[]{0.06f, 0.03f});

        // Damage half covers every weapon family. Energy projectile weapons (pulse, ion, plasma)
        // were left out; energy beams are covered by the energy multiplier.
        assert touched.contains("getEnergyWeaponDamageMult") : "energy weapons not boosted";
        assert touched.contains("getBallisticWeaponDamageMult");
        assert touched.contains("getMissileWeaponDamageMult");
        assert !touched.contains("getBeamWeaponDamageMult") : "energy beams would double-dip";
        // Defensive half unchanged.
        assert touched.contains("getHullDamageTakenMult");
        assert touched.contains("getArmorDamageTakenMult");
        assert touched.contains("getEmpDamageTakenMult");

        System.out.println("ALL PASSED");
    }
}
