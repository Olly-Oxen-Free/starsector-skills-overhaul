package skills_overhaul.zealot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ZealotCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<>();

    private final AvengeState avenge = new AvengeState();
    private final TributeState tribute = new TributeState();
    private final DeathCheatState deathCheat = new DeathCheatState();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        if (ACTIVE_NODES.contains("zealot_t2_avenge")) avenge.tick(engine);
        if (ACTIVE_NODES.contains("zealot_t4_tribute")) tribute.tick(engine);

        int zealStacks = ZealService.getStacks(Global.getSector().getMemoryWithoutUpdate());
        Oath currentOath = OathService.getCurrentOath(Global.getSector().getMemoryWithoutUpdate());

        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            applyPerShipEffects(s, engine, zealStacks, currentOath);
            if (ACTIVE_NODES.contains("zealot_t4_death_cheat")) deathCheat.tick(s, zealStacks);
        }
    }

    private void applyPerShipEffects(ShipAPI s, CombatEngineAPI engine, int zealStacks, Oath oath) {
        // T0 base effects: always-on if T0 base purchased (no ACTIVE_NODES gate)
        if (Boolean.TRUE.equals(com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate().get(
                skills_overhaul.effect.handlers.SOZealotT0Handler.K_PURCHASED))) {
            T0BallisticRange.apply(s);
            T0CapCruiserArmor.apply(s);
        }
        if (ACTIVE_NODES.contains("zealot_t1_hammerfall")) Hammerfall.apply(s, oath);
        if (ACTIVE_NODES.contains("zealot_t1_disciplined_gunnery")) DisciplinedGunnery.apply(s, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t1_damage_control")) DamageControl.apply(s, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t2_avenge")) avenge.applyToShip(s);
        if (ACTIVE_NODES.contains("zealot_t2_reinforced_plating")) ReinforcedPlating.apply(s, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t2_battle_line")) BattleLine.apply(s, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t3_steelbound")) Steelbound.apply(s, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t3_capital_doctrine")) CapitalDoctrine.apply(s, engine, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t3_last_stand")) LastStand.apply(s, zealStacks);
        if (ACTIVE_NODES.contains("zealot_t3_iron_discipline")) IronDiscipline.apply(s);
        if (ACTIVE_NODES.contains("zealot_t4_sanctum")) Sanctum.apply(s, engine);
    }

    static final class T0BallisticRange {
        static void apply(ShipAPI s) {
            s.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("zealot_t0_base_range", 5f);
        }
    }

    static final class T0CapCruiserArmor {
        static void apply(ShipAPI s) {
            if (s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP
                    || s.getHullSize() == ShipAPI.HullSize.CRUISER) {
                s.getMutableStats().getArmorBonus().modifyFlat("zealot_t0_base_armor", 25f);
            }
        }
    }

    static final class Hammerfall {
        static void apply(ShipAPI s, Oath oath) {
            if (oath == Oath.SINGULARITY) {
                s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("zealot_t1_hammerfall", 1.10f);
                s.getMutableStats().getBeamWeaponRangeBonus().modifyPercent("zealot_t1_hammerfall", 5f);
            } else {
                s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("zealot_t1_hammerfall", 1.05f);
            }
        }
    }

    static final class DisciplinedGunnery {
        static void apply(ShipAPI s, int zealStacks) {
            s.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult("zealot_t1_disciplined", 0.90f);
            s.getMutableStats().getBallisticAmmoBonus().modifyMult("zealot_t1_disciplined", 1.25f);
            float ammoRegenBonus = 1f + Math.min(0.05f, 0.0005f * zealStacks);
            s.getMutableStats().getBallisticAmmoRegenMult().modifyMult("zealot_t1_disciplined", ammoRegenBonus);
        }
    }

    static final class DamageControl {
        static void apply(ShipAPI s, int zealStacks) {
            float perStackBonus = Math.min(0.005f, 0.00005f * zealStacks);
            s.getMutableStats().getHullDamageTakenMult().modifyMult(
                    "zealot_t1_damage_control", 1f - 0.05f - perStackBonus);
        }
    }

    static final class AvengeState {
        Set<ShipAPI> lastSeen = new HashSet<>();
        int losses = 0;

        void tick(CombatEngineAPI engine) {
            Set<ShipAPI> alive = new HashSet<>();
            for (ShipAPI o : engine.getShips()) if (o.isAlive() && o.getOwner() == 0) alive.add(o);
            for (ShipAPI prev : lastSeen) {
                if (!alive.contains(prev)) losses = Math.min(5, losses + 1);
            }
            lastSeen = alive;
        }

        void applyToShip(ShipAPI s) {
            float damageBonus = 1f + 0.03f * losses;
            float dissipationBonus = 1f + 0.05f * losses;
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("zealot_t2_avenge", damageBonus);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("zealot_t2_avenge", damageBonus);
            s.getMutableStats().getFluxDissipation().modifyMult("zealot_t2_avenge", dissipationBonus);
        }
    }

    static final class ReinforcedPlating {
        static void apply(ShipAPI s, int zealStacks) {
            float armorBonus = 50f + 0.2f * Math.min(100, zealStacks);
            s.getMutableStats().getArmorBonus().modifyFlat("zealot_t2_reinforced", armorBonus);
            s.getMutableStats().getEmpDamageTakenMult().modifyMult("zealot_t2_reinforced", 0.50f);
        }
    }

    static final class BattleLine {
        static void apply(ShipAPI s, int zealStacks) {
            if (s.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP
                    && s.getHullSize() != ShipAPI.HullSize.CRUISER) return;
            s.getMutableStats().getMaxSpeed().modifyMult("zealot_t2_battle_line", 0.95f);
            s.getMutableStats().getArmorBonus().modifyFlat("zealot_t2_battle_line", 75f);
            float rangePct = Math.min(10f, 0.1f * zealStacks);
            if (s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                s.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("zealot_t2_battle_line", rangePct);
            }
        }
    }

    static final class Steelbound {
        static void apply(ShipAPI s, int zealStacks) {
            if (s.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP
                    && s.getHullSize() != ShipAPI.HullSize.CRUISER) return;
            s.getMutableStats().getHullBonus().modifyMult("zealot_t3_steelbound", 1.10f);
            float critProxy = 0.05f + Math.min(0.075f, 0.00075f * zealStacks);
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("zealot_t3_steelbound_armor", 1f + critProxy);
        }
    }

    static final class CapitalDoctrine {
        static void apply(ShipAPI s, CombatEngineAPI engine, int zealStacks) {
            int caps = 0;
            for (ShipAPI o : engine.getShips()) {
                if (o.getOwner() != 0) continue;
                if (o.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) caps++;
            }
            int effective = Math.min(3, caps);
            float rangePct = 3f * effective;
            float hePct = 5f * effective;
            if (s.getHullSize() == ShipAPI.HullSize.CRUISER || s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                s.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("zealot_t3_capital_doctrine", rangePct);
                s.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("zealot_t3_capital_doctrine", rangePct);
                s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("zealot_t3_capital_doctrine", 1f + hePct / 100f);
            }
        }
    }

    static final class LastStand {
        static void apply(ShipAPI s, int zealStacks) {
            float threshold = 0.30f + Math.min(0.20f, 0.002f * zealStacks);
            float hullPct = s.getHitpoints() / Math.max(1f, s.getMaxHitpoints());
            if (hullPct >= threshold) {
                s.getMutableStats().getArmorBonus().unmodify("zealot_t3_last_stand");
                s.getMutableStats().getFluxDissipation().unmodify("zealot_t3_last_stand");
                s.getMutableStats().getPeakCRDuration().unmodify("zealot_t3_last_stand");
                return;
            }
            s.getMutableStats().getArmorBonus().modifyFlat("zealot_t3_last_stand", 50f);
            s.getMutableStats().getFluxDissipation().modifyMult("zealot_t3_last_stand", 1.25f);
            s.getMutableStats().getPeakCRDuration().modifyMult("zealot_t3_last_stand", 1.25f);
        }
    }

    static final class IronDiscipline {
        static void apply(ShipAPI s) {
            float hullPct = s.getHitpoints() / Math.max(1f, s.getMaxHitpoints());
            float scale = 1f + 0.5f * (1f - Math.max(0.5f, hullPct)) / 0.5f;
            float baseBonus = 0.15f * scale;
            float pdBonus = baseBonus * 2f;
            s.getMutableStats().getBallisticRoFMult().modifyMult("zealot_t3_iron_discipline", 1f + baseBonus);
            s.getMutableStats().getEnergyRoFMult().modifyMult("zealot_t3_iron_discipline", 1f + baseBonus);
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("zealot_t3_iron_discipline_acc", 1f + baseBonus);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("zealot_t3_iron_discipline_acc", 1f + baseBonus);
            s.getMutableStats().getEnergyAmmoRegenMult().modifyMult("zealot_t3_iron_discipline_pd", 1f + pdBonus);
        }
    }

    static final class TributeState {
        Set<ShipAPI> lastSeen = new HashSet<>();
        Map<ShipAPI, Float> hullRepairAccum = new HashMap<>();
        Map<ShipAPI, Float> fluxAccum = new HashMap<>();
        Map<ShipAPI, Float> armorAccum = new HashMap<>();

        void tick(CombatEngineAPI engine) {
            Set<ShipAPI> alive = new HashSet<>();
            for (ShipAPI o : engine.getShips()) if (o.isAlive()) alive.add(o);
            for (ShipAPI prev : lastSeen) {
                if (!alive.contains(prev) && prev.getOwner() == 1) {
                    grantTribute(engine, prev);
                }
            }
            lastSeen = alive;
        }

        void grantTribute(CombatEngineAPI engine, ShipAPI killed) {
            float hullPct, fluxPct, armorPct;
            switch (killed.getHullSize()) {
                case FRIGATE:    hullPct = 0.05f; fluxPct = 0.01f; armorPct = 0.025f; break;
                case DESTROYER:  hullPct = 0.08f; fluxPct = 0.02f; armorPct = 0.04f;  break;
                case CRUISER:    hullPct = 0.12f; fluxPct = 0.03f; armorPct = 0.06f;  break;
                case CAPITAL_SHIP: hullPct = 0.20f; fluxPct = 0.05f; armorPct = 0.10f; break;
                default: return;
            }
            boolean isSingularity = OathService.getCurrentOath(Global.getSector().getMemoryWithoutUpdate()) == Oath.SINGULARITY;
            for (ShipAPI o : engine.getShips()) {
                if (o.getOwner() != 0 || !o.isAlive()) continue;
                Float currentHull = hullRepairAccum.get(o);
                if (currentHull == null) currentHull = 0f;
                if (currentHull < 0.50f) {
                    float toAdd = Math.min(hullPct, 0.50f - currentHull);
                    o.setHitpoints(Math.min(o.getMaxHitpoints(), o.getHitpoints() + o.getMaxHitpoints() * toAdd));
                    hullRepairAccum.put(o, currentHull + toAdd);
                }
                Float currentFlux = fluxAccum.get(o);
                if (currentFlux == null) currentFlux = 0f;
                float effectiveFluxPct = isSingularity ? fluxPct * 2f : fluxPct;
                if (currentFlux < 0.25f) {
                    float toAdd = Math.min(effectiveFluxPct, 0.25f - currentFlux);
                    o.getMutableStats().getFluxCapacity().modifyMult("zealot_t4_tribute", 1f + currentFlux + toAdd);
                    fluxAccum.put(o, currentFlux + toAdd);
                }
                if (!isSingularity) {
                    Float currentArmor = armorAccum.get(o);
                    if (currentArmor == null) currentArmor = 0f;
                    float newArmor = currentArmor + armorPct * 100f;
                    o.getMutableStats().getArmorBonus().modifyFlat("zealot_t4_tribute_armor", newArmor);
                    armorAccum.put(o, newArmor);
                }
            }
        }
    }

    static final class DeathCheatState {
        Set<ShipAPI> triggered = new HashSet<>();

        void tick(ShipAPI s, int zealStacks) {
            if (triggered.contains(s)) return;
            float hullPct = s.getHitpoints() / Math.max(1f, s.getMaxHitpoints());
            if (hullPct > 0.04f) return;
            triggered.add(s);
            float duration = 5f + Math.min(5f, 0.05f * zealStacks);
            // TODO: ShipAPI.setInvincible not available in this API version; replace with a hull/damage modifier if added later
            for (WeaponAPI w : s.getAllWeapons()) {
                try { w.repair(); } catch (Throwable t) {}
            }
            s.getFluxTracker().setHardFlux(0f);
            final ShipAPI ship = s;
            final float dur = duration;
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                float remaining = dur;
                @Override public void advance(float amount, List<InputEventAPI> events) {
                    remaining -= amount;
                    if (remaining <= 0) {
                        // TODO: ship.setInvincible(false) when API supports it
                        Global.getCombatEngine().removePlugin(this);
                    }
                }
            });
        }
    }

    static final class Sanctum {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            ShipAPI flagship = engine.getPlayerShip();
            if (flagship == null) return;
            if (s == flagship) {
                s.getMutableStats().getPeakCRDuration().modifyMult("zealot_t4_sanctum_flag", 1.25f);
                s.getMutableStats().getArmorBonus().modifyMult("zealot_t4_sanctum_flag", 1.25f);
            } else if (Misc.getDistance(s.getLocation(), flagship.getLocation()) <= 2000f) {
                s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("zealot_t4_sanctum_aura", 1.10f);
                s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("zealot_t4_sanctum_aura", 1.10f);
            }
        }
    }
}
