package skills_overhaul.artificer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ArtificerCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<>();

    private final BeamSustainedContactTracker beamTracker = new BeamSustainedContactTracker();
    private boolean firstFrame = true;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        if (firstFrame) {
            FinalGambitService.resetForBattle();
            firstFrame = false;
        }

        if (ACTIVE_NODES.contains("artificer_t3_final_gambit")) {
            FinalGambitService.tick(engine);
        }

        // Per-ship effect dispatch
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            applyPerShipEffects(s, engine, amount);
        }
    }

    private void applyPerShipEffects(ShipAPI s, CombatEngineAPI engine, float amount) {
        // T1
        if (ACTIVE_NODES.contains("artificer_t1_ecm")) ECM.apply(s);
        if (ACTIVE_NODES.contains("artificer_t1_ewm")) EnergyWeaponMastery.apply(s);
        if (ACTIVE_NODES.contains("artificer_t1_systems")) SystemsExpertise.apply(s);
        // T2
        if (ACTIVE_NODES.contains("artificer_t2_optics")) PrecisionOptics.apply(s);
        if (ACTIVE_NODES.contains("artificer_t2_strike")) StrikePackage.apply(s);
        if (ACTIVE_NODES.contains("artificer_t2_shield")) ShieldMastery.apply(s);
        if (ACTIVE_NODES.contains("artificer_t2_standard_systems")) StandardisedSystems.apply(s);
        // T3
        if (ACTIVE_NODES.contains("artificer_t3_beam_spec")) BeamSpec.apply(s, amount, beamTracker);
        if (ACTIVE_NODES.contains("artificer_t3_adv_strike")) AdvStrike.apply(s);
        if (ACTIVE_NODES.contains("artificer_t3_resilience")) TechResilience.apply(s);
        if (ACTIVE_NODES.contains("artificer_t3_power_grid")) PowerGrid.apply(s, engine);
        // T4
        if (ACTIVE_NODES.contains("artificer_t4_transcendence")) TechTranscendence.apply(s, engine);
        if (ACTIVE_NODES.contains("artificer_t4_carrier_command")) CarrierCommand.apply(s);
        if (ACTIVE_NODES.contains("artificer_t4_limit_breaker")) LimitBreaker.apply(s);
    }

    // ---------- Effect inner classes ----------

    static final class ECM {
        static void apply(ShipAPI s) {
            float adv = ECMTracker.advantage(
                    ECMTracker.getPlayerECMRatingLive(),
                    ECMTracker.getEnemyECMRatingLive());
            float enemyRangePenalty = ECMTracker.enemyWeaponRangePenaltyMult(adv);
            float rangePct = (1f - enemyRangePenalty) * 100f;
            s.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("artificer_t1_ecm_range", rangePct);
            s.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("artificer_t1_ecm_range", rangePct);
        }
    }

    static final class EnergyWeaponMastery {
        static void apply(ShipAPI s) {
            try {
                s.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult("artificer_t1_ewm", 0.85f);
            } catch (Throwable t) {}
            s.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("artificer_t1_ewm", 15f);
        }
    }

    static final class SystemsExpertise {
        static void apply(ShipAPI s) {
            try {
                s.getMutableStats().getSystemRegenBonus().modifyMult("artificer_t1_systems", 1.25f);
            } catch (Throwable t) {}
        }
    }

    static final class PrecisionOptics {
        static void apply(ShipAPI s) {
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("artificer_t2_optics", 1.10f);
            s.getMutableStats().getBeamWeaponRangeBonus().modifyFlat("artificer_t2_optics", 100f);
        }
    }

    static final class StrikePackage {
        static void apply(ShipAPI s) {
            if (s.getPhaseCloak() != null) {
                try { s.getMutableStats().getPhaseCloakActivationCostBonus().modifyMult("artificer_t2_strike", 0.80f); }
                catch (Throwable t) {}
            }
            try {
                if (s.getHullSpec().hasTag("automated")) {
                    s.getMutableStats().getEmpDamageTakenMult().modifyMult("artificer_t2_strike", 0.80f);
                    s.getMutableStats().getShieldDamageTakenMult().modifyMult("artificer_t2_strike", 0.90f);
                }
            } catch (Throwable t) {}
            if (s.getNumFighterBays() > 0) {
                try {
                    s.getMutableStats().getFighterRefitTimeMult().modifyMult("artificer_t2_strike", 0.70f);
                } catch (Throwable t) {}
            }
        }
    }

    static final class ShieldMastery {
        static void apply(ShipAPI s) {
            s.getMutableStats().getShieldDamageTakenMult().modifyMult("artificer_t2_shield", 0.85f);
            try { s.getMutableStats().getShieldUpkeepMult().modifyMult("artificer_t2_shield", 0.75f); }
            catch (Throwable t) {}
        }
    }

    static final class StandardisedSystems {
        static void apply(ShipAPI s) {
            s.getMutableStats().getBallisticAmmoRegenMult().modifyMult("artificer_t2_standard", 1.50f);
            s.getMutableStats().getEnergyAmmoRegenMult().modifyMult("artificer_t2_standard", 1.50f);
            if (s.getCaptain() == null || s.getCaptain().isDefault()) {
                s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("artificer_t2_standard_implants", 1.05f);
                s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("artificer_t2_standard_implants", 1.05f);
            }
        }
    }

    static final class BeamSpec {
        static void apply(ShipAPI s, float amount, BeamSustainedContactTracker tracker) {
            s.getMutableStats().getBeamWeaponDamageMult().modifyMult("artificer_t3_beam_spec", 1.20f);
            float fluxFraction = s.getFluxLevel();
            float rofMult = fluxFraction > 0.5f ? 1.20f : 1.10f;
            s.getMutableStats().getEnergyRoFMult().modifyMult("artificer_t3_beam_spec", rofMult);

            // Beam tracker: per-ship approximation — firing beams accumulate contact time vs a sentinel target.
            boolean anyBeamFiring = false;
            for (com.fs.starfarer.api.combat.WeaponAPI w : s.getAllWeapons()) {
                if (w.getType() != com.fs.starfarer.api.combat.WeaponAPI.WeaponType.MISSILE && w.isFiring()) {
                    anyBeamFiring = true;
                    break;
                }
            }
            if (anyBeamFiring) {
                tracker.onBeamHit(s, s, amount);
            } else {
                tracker.onBeamMissed(s, s);
            }
        }
    }

    static final class AdvStrike {
        static void apply(ShipAPI s) {
            if (s.getPhaseCloak() != null) {
                s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("artificer_t3_adv_strike", 1.10f);
                s.getMutableStats().getMaxSpeed().modifyMult("artificer_t3_adv_strike", 1.10f);
            }
        }
    }

    static final class TechResilience {
        static void apply(ShipAPI s) {
            try {
                if (s.getHullSpec().hasTag("automated")) {
                    s.getMutableStats().getHullDamageTakenMult().modifyMult("artificer_t3_resilience", 0.90f);
                }
            } catch (Throwable t) {}
        }
    }

    static final class PowerGrid {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            if (s.getFluxTracker() != null && s.getFluxTracker().isVenting()) {
                s.getMutableStats().getMaxSpeed().modifyMult("artificer_t3_power_grid_vent", 1.30f);
                s.getMutableStats().getMaxTurnRate().modifyMult("artificer_t3_power_grid_vent", 1.30f);
            } else {
                s.getMutableStats().getMaxSpeed().unmodify("artificer_t3_power_grid_vent");
                s.getMutableStats().getMaxTurnRate().unmodify("artificer_t3_power_grid_vent");
            }
            float fluxLevel = s.getFluxLevel();
            if (fluxLevel < 0.10f) {
                s.getMutableStats().getShieldDamageTakenMult().modifyMult("artificer_t3_power_grid_low", 0.90f);
            } else {
                s.getMutableStats().getShieldDamageTakenMult().unmodify("artificer_t3_power_grid_low");
            }
            float minDist = Float.MAX_VALUE;
            for (ShipAPI o : engine.getShips()) {
                if (o.getOwner() != 1 || !o.isAlive()) continue;
                float d = Misc.getDistance(s.getLocation(), o.getLocation());
                if (d < minDist) minDist = d;
            }
            float proximityMult = 1f;
            if (minDist < 800f) proximityMult = 1f + 0.20f * (1f - minDist / 800f);
            s.getMutableStats().getFluxDissipation().modifyMult("artificer_t3_power_grid_prox", proximityMult);
        }
    }

    static final class TechTranscendence {
        static void apply(ShipAPI s, CombatEngineAPI engine) {
            if (s.getPhaseCloak() == null) return;
            float bonus = PhaseSharedBonusService.computeBonusForLivePhaseFleet();
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("artificer_t4_transcendence", 1f + bonus);
            s.getMutableStats().getFluxDissipation().modifyMult("artificer_t4_transcendence", 1f + bonus);
        }
    }

    static final class CarrierCommand {
        static void apply(ShipAPI s) {
            if (s.getNumFighterBays() <= 0) return;
            try {
                s.getMutableStats().getFighterRefitTimeMult().modifyMult("artificer_t4_carrier", 0.80f);
            } catch (Throwable t) {}
        }
    }

    static final class LimitBreaker {
        static void apply(ShipAPI s) {
            try {
                if (!s.getHullSpec().hasTag("automated")) return;
            } catch (Throwable t) { return; }
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("artificer_t4_limit_breaker", 1.20f);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("artificer_t4_limit_breaker", 1.20f);
            s.getMutableStats().getMaxSpeed().modifyMult("artificer_t4_limit_breaker", 1.20f);
            s.getMutableStats().getArmorBonus().modifyMult("artificer_t4_limit_breaker", 1.20f);
        }
    }
}
