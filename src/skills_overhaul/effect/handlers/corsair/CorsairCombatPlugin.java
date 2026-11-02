package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combat plugin that applies Corsair node effects per-frame to player-side ships.
 * Effects gate on entries in {@link #ACTIVE_NODES}, populated by CombatScriptHandler.apply().
 */
public final class CorsairCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<>();

    private float combatTime = 0f;
    /** Ships carrying the Trapped Prey debuff last frame / this frame. */
    private final Set<ShipAPI> trappedPrey = new HashSet<>();
    private final Set<ShipAPI> trappedPreyNow = new HashSet<>();

    @Override
    public void init(CombatEngineAPI engine) {
        combatTime = 0f;
        BloodInTheWater.reset();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        // ACTIVE_NODES is campaign state; keep it out of title-screen/mission battles.
        if (!engine.isInCampaign() && !engine.isInCampaignSim()) return;
        combatTime += amount;
        trappedPreyNow.clear();
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            applyPerShipEffects(s, engine);
        }
        // Strip Trapped Prey from ships no longer targeted, or the debuff sticks.
        for (ShipAPI prey : trappedPrey) {
            if (!trappedPreyNow.contains(prey)) TrappedPrey.remove(prey);
        }
        trappedPrey.clear();
        trappedPrey.addAll(trappedPreyNow);
        // Blood-in-the-Water tick is global, not per-ship.
        if (ACTIVE_NODES.contains("corsair_t4_blood_in_water")) {
            BloodInTheWater.tickGlobal(engine);
        }
    }

    private void applyPerShipEffects(ShipAPI s, CombatEngineAPI engine) {
        if (ACTIVE_NODES.contains("corsair_t1_scrappy"))       ScrappyMaintenance.apply(s);
        if (ACTIVE_NODES.contains("corsair_t1_all_out"))       AllOut.apply(s);
        if (ACTIVE_NODES.contains("corsair_t2_wolfpack"))      Wolfpack.apply(s);
        if (ACTIVE_NODES.contains("corsair_t2_jumpstart")) {
            // First 30s only — the bonus was never removed afterwards.
            if (combatTime <= 30f) Jumpstart.apply(s);
            else s.getMutableStats().getMaxSpeed().unmodify("corsair_t2_jumpstart");
        }
        if (ACTIVE_NODES.contains("corsair_t3_pack_hunter"))   PackHunter.apply(s, engine);
        if (ACTIVE_NODES.contains("corsair_t3_trapped_prey"))  TrappedPrey.apply(s, trappedPreyNow);
        if (ACTIVE_NODES.contains("corsair_t3_hunting_grounds")) HuntingGrounds.apply(s);
        if (ACTIVE_NODES.contains("corsair_t4_quick_wind"))    QuickAsTheWind.apply(s);
        if (ACTIVE_NODES.contains("corsair_t4_shifting_tides")) ShiftingTides.apply(s, engine);
        if (ACTIVE_NODES.contains("corsair_t4_blood_in_water")) BloodInTheWater.applyShip(s, engine);
    }

    // ---------- Effect implementations ----------

    /**
     * +2% CR per d-mod (cap 5); CR-per-deployment +5% per d-mod (cap 5).
     * getCRPerDeploymentPercent confirmed present in API jar.
     */
    /** Hunting Grounds: frigates/destroyers +15% speed, +10% maneuverability (vanilla Helmsmanship stats). */
    static final class HuntingGrounds {
        static final String ID = "corsair_t3_hunting_grounds";
        static void apply(ShipAPI s) {
            ShipAPI.HullSize h = s.getHullSize();
            if (h != ShipAPI.HullSize.FRIGATE && h != ShipAPI.HullSize.DESTROYER) return;
            s.getMutableStats().getMaxSpeed().modifyPercent(ID, 15f);
            s.getMutableStats().getAcceleration().modifyPercent(ID, 10f);
            s.getMutableStats().getDeceleration().modifyPercent(ID, 10f);
            s.getMutableStats().getTurnAcceleration().modifyPercent(ID, 10f);
            s.getMutableStats().getMaxTurnRate().modifyPercent(ID, 10f);
        }
    }

    static final class ScrappyMaintenance {
        static void apply(ShipAPI s) {
            int eff = countDMods(s);
            s.getMutableStats().getMaxCombatReadiness().modifyFlat("corsair_t1_scrappy", eff * 0.02f);
            s.getMutableStats().getCRPerDeploymentPercent().modifyFlat("corsair_t1_scrappy", eff * 5f);
        }
    }

    /** +20% non-missile ammo capacity, +25% regen. */
    static final class AllOut {
        static void apply(ShipAPI s) {
            s.getMutableStats().getBallisticAmmoBonus().modifyMult("corsair_t1_all_out", 1.20f);
            s.getMutableStats().getEnergyAmmoBonus().modifyMult("corsair_t1_all_out", 1.20f);
            s.getMutableStats().getBallisticAmmoRegenMult().modifyMult("corsair_t1_all_out", 1.20f);
            s.getMutableStats().getEnergyAmmoRegenMult().modifyMult("corsair_t1_all_out", 1.20f);
        }
    }

    /** Frigates +20% damage to non-frigates + 50% PPT. Destroyers +10% / +25% PPT. */
    static final class Wolfpack {
        static void apply(ShipAPI s) {
            switch (s.getHullSize()) {
                case FRIGATE:
                    s.getMutableStats().getPeakCRDuration().modifyMult("corsair_t2_wolfpack", 1.50f);
                    s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("corsair_t2_wolfpack", 1.20f);
                    s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("corsair_t2_wolfpack", 1.20f);
                    break;
                case DESTROYER:
                    s.getMutableStats().getPeakCRDuration().modifyMult("corsair_t2_wolfpack", 1.25f);
                    s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("corsair_t2_wolfpack", 1.10f);
                    s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("corsair_t2_wolfpack", 1.10f);
                    break;
                default: break;
            }
        }
    }

    /** First 30s: frigate/destroyer +20% max speed. */
    static final class Jumpstart {
        static void apply(ShipAPI s) {
            if (s.getHullSize() == ShipAPI.HullSize.FRIGATE
                    || s.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                s.getMutableStats().getMaxSpeed().modifyMult("corsair_t2_jumpstart", 1.20f);
            }
        }
    }

    /** +3% speed/flux/damage per nearby friendly frigate within 2000u. */
    static final class PackHunter {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            int nearby = 0;
            for (ShipAPI o : engine.getShips()) {
                if (o == s || o.getOwner() != s.getOwner()) continue;
                if (o.getHullSize() != ShipAPI.HullSize.FRIGATE) continue;
                if (com.fs.starfarer.api.util.Misc.getDistance(s.getLocation(), o.getLocation()) <= 2000f) nearby++;
            }
            float k = 0.03f * nearby;
            s.getMutableStats().getMaxSpeed().modifyMult("corsair_t3_pack_hunter", 1f + k);
            s.getMutableStats().getFluxDissipation().modifyMult("corsair_t3_pack_hunter", 1f + k);
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("corsair_t3_pack_hunter", 1f + k);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("corsair_t3_pack_hunter", 1f + k);
        }
    }

    /** Player ship's current target: −15% maneuver speed/turn, +5% hull damage taken. */
    static final class TrappedPrey {
        static void apply(ShipAPI s, Set<ShipAPI> marked) {
            ShipAPI tgt = s.getShipTarget();
            if (tgt == null || tgt.getOwner() == s.getOwner()) return;
            tgt.getMutableStats().getMaxSpeed().modifyMult("corsair_t3_trapped_prey", 0.85f);
            // getMaxTurnRate confirmed present in API jar
            tgt.getMutableStats().getMaxTurnRate().modifyMult("corsair_t3_trapped_prey", 0.85f);
            tgt.getMutableStats().getHullDamageTakenMult().modifyMult("corsair_t3_trapped_prey", 1.05f);
            marked.add(tgt);
        }

        static void remove(ShipAPI tgt) {
            tgt.getMutableStats().getMaxSpeed().unmodify("corsair_t3_trapped_prey");
            tgt.getMutableStats().getMaxTurnRate().unmodify("corsair_t3_trapped_prey");
            tgt.getMutableStats().getHullDamageTakenMult().unmodify("corsair_t3_trapped_prey");
        }
    }

    /**
     * System cooldown reduction by hull size.
     * Uses getSystemCooldownBonus (vanilla SystemsExpertise pattern). getSystemRegenBonus
     * only speeds charge regeneration, so it did nothing for cooldown-based systems.
     */
    static final class QuickAsTheWind {
        static void apply(ShipAPI s) {
            float reduction;
            switch (s.getHullSize()) {
                case FRIGATE:      reduction = 0.33f; break;
                case DESTROYER:    reduction = 0.25f; break;
                case CRUISER:      reduction = 0.10f; break;
                case CAPITAL_SHIP: reduction = 0.05f; break;
                default:           reduction = 0f;
            }
            if (reduction > 0f) {
                s.getMutableStats().getSystemCooldownBonus()
                        .modifyMult("corsair_t4_quick_wind", 1f - reduction);
            }
        }
    }

    /**
     * Ratio = playerDP / enemyDP (using getCurrStrength() as proxy for deployed fleet points;
     * getDeployedFleetPoints() does not exist in this API version).
     * ≤0.5 → +20% armor/dissipation. ≥1.5 → +20% speed/damage.
     */
    static final class ShiftingTides {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            float playerDP = engine.getFleetManager(FleetSide.PLAYER).getCurrStrength();
            float enemyDP  = engine.getFleetManager(FleetSide.ENEMY).getCurrStrength();
            float ratio = enemyDP <= 0f ? 1.5f : playerDP / enemyDP;

            float speedAndDmg;
            float armorAndFlux;
            if (ratio <= 0.5f) {
                speedAndDmg = 0f; armorAndFlux = 0.20f;
            } else if (ratio >= 1.5f) {
                speedAndDmg = 0.20f; armorAndFlux = 0f;
            } else if (ratio < 1f) {
                armorAndFlux = lerp(0.20f, 0f, (ratio - 0.5f) / 0.5f); speedAndDmg = 0f;
            } else {
                speedAndDmg = lerp(0f, 0.20f, (ratio - 1f) / 0.5f); armorAndFlux = 0f;
            }

            s.getMutableStats().getMaxSpeed().modifyMult("corsair_t4_shifting_tides", 1f + speedAndDmg);
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("corsair_t4_shifting_tides", 1f + speedAndDmg);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("corsair_t4_shifting_tides", 1f + speedAndDmg);
            // getArmorBonus confirmed present in API jar
            s.getMutableStats().getArmorBonus().modifyMult("corsair_t4_shifting_tides", 1f + armorAndFlux);
            s.getMutableStats().getFluxDissipation().modifyMult("corsair_t4_shifting_tides", 1f + armorAndFlux);
        }

        static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    }

    /**
     * On enemy kill: friendly frigates within 2000u of kill site get +20% speed, +15% damage for 8s.
     * Uses static state; reset() called in init() each combat.
     */
    static final class BloodInTheWater {
        static Set<ShipAPI> lastSeen = new HashSet<>();
        static Map<ShipAPI, Float> buffUntil = new HashMap<>();

        static void reset() {
            lastSeen.clear();
            buffUntil.clear();
        }

        static void tickGlobal(CombatEngineAPI engine) {
            Set<ShipAPI> alive = new HashSet<>();
            for (ShipAPI o : engine.getShips()) {
                if (o.isAlive()) alive.add(o);
            }
            for (ShipAPI prev : lastSeen) {
                // Destroyed ships become hulks with owner 100, so test the original owner;
                // !isAlive() excludes retreated ships, which also leave getShips().
                if (!alive.contains(prev) && !prev.isAlive() && prev.getOriginalOwner() == 1) {
                    // This ship was alive last tick and is now destroyed — count as enemy kill.
                    for (ShipAPI ally : engine.getShips()) {
                        if (ally.getOwner() != 0 || ally.getHullSize() != ShipAPI.HullSize.FRIGATE) continue;
                        if (com.fs.starfarer.api.util.Misc.getDistance(ally.getLocation(), prev.getLocation()) <= 2000f) {
                            buffUntil.put(ally, engine.getTotalElapsedTime(false) + 8f);
                        }
                    }
                }
            }
            lastSeen = alive;
        }

        static void applyShip(ShipAPI s, CombatEngineAPI engine) {
            Float until = buffUntil.get(s);
            if (until != null && engine.getTotalElapsedTime(false) < until) {
                s.getMutableStats().getMaxSpeed().modifyMult("corsair_t4_blood", 1.20f);
                s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("corsair_t4_blood", 1.15f);
                s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("corsair_t4_blood", 1.15f);
            } else if (until != null) {
                s.getMutableStats().getMaxSpeed().unmodify("corsair_t4_blood");
                s.getMutableStats().getBallisticWeaponDamageMult().unmodify("corsair_t4_blood");
                s.getMutableStats().getEnergyWeaponDamageMult().unmodify("corsair_t4_blood");
                buffUntil.remove(s);
            }
        }
    }

    // ---------- Helpers ----------

    /**
     * Counts d-mods on the ship's variant via the vanilla "dmod" hullmod tag. The old id
     * heuristic ("_dmod"/"_d_"/"dmods_") matched no vanilla d-mod id, so it always returned 0.
     * Cap at 5.
     */
    private static int countDMods(ShipAPI s) {
        return Math.min(5, com.fs.starfarer.api.impl.campaign.DModManager.getNumDMods(s.getVariant()));
    }
}
