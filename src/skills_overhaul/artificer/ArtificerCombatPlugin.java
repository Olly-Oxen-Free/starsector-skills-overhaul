package skills_overhaul.artificer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArtificerCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<>();

    /** Enemy Officer Imprint expiry key — mirrored in OfficerImprintDevice. */
    public static final String OFFICER_IMPRINT_EXPIRE_KEY = "$skills.artificer.forge.officer_imprint_expire";
    private static final String BEAM_DEBUFF_ID = "artificer_t3_beam_spec_debuff";
    private static final String OFFICER_IMPRINT_ID = "artificer_officer_imprint";

    private final BeamSustainedContactTracker beamTracker = new BeamSustainedContactTracker();
    /** Targets that currently carry the beam flux-dissipation debuff (for clean unmodify). */
    private final Set<Object> beamDebuffTargets = new HashSet<Object>();
    private boolean firstFrame = true;
    private float ecmRangePct = 0f;
    private float phaseBonus = 0f;

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

        // Fleet-wide inputs: compute once per frame, not once per ship (each is a full-fleet scan).
        ecmRangePct = ACTIVE_NODES.contains("artificer_t1_ecm") ? ECM.computeRangePct() : 0f;
        phaseBonus = ACTIVE_NODES.contains("artificer_t4_transcendence")
                ? PhaseSharedBonusService.computeBonusForLivePhaseFleet() : 0f;

        // Per-ship effect dispatch
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            applyPerShipEffects(s, engine, amount);
        }

        // Beam Specialization — sustained-contact flux dissipation debuff (fleet-wide, beam-source driven).
        if (ACTIVE_NODES.contains("artificer_t3_beam_spec")) {
            processBeamContact(engine, amount);
        }

        // Officer Imprint — device-driven (timed campaign flag), buffs officer-led fighter wings.
        if (isOfficerImprintActive()) {
            applyOfficerImprint(engine);
        }
    }

    /** Accumulate per-target beam contact for friendly beam sources and apply the flux-dissipation debuff. */
    private void processBeamContact(CombatEngineAPI engine, float amount) {
        // Gather the targets each friendly beam source is damaging this frame.
        Map<ShipAPI, Set<Object>> currentBySource = new HashMap<ShipAPI, Set<Object>>();
        Set<Object> targetsHitThisFrame = new HashSet<Object>();
        for (BeamAPI beam : engine.getBeams()) {
            ShipAPI src = beam.getSource();
            if (src == null || src.getOwner() != 0) continue;
            CombatEntityAPI tgt = beam.getDamageTarget();
            if (tgt == null || !beam.didDamageThisFrame()) continue;
            Set<Object> set = currentBySource.get(src);
            if (set == null) { set = new HashSet<Object>(); currentBySource.put(src, set); }
            set.add(tgt);
            targetsHitThisFrame.add(tgt);
        }

        // Advance contact time; reset targets a source lost this frame.
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            Set<Object> current = currentBySource.get(s);
            if (current == null) current = java.util.Collections.emptySet();
            for (Object prev : beamTracker.getTargets(s)) {
                if (!current.contains(prev)) beamTracker.onBeamMissed(s, prev);
            }
            for (Object tgt : current) {
                beamTracker.onBeamHit(s, tgt, amount);
            }
        }

        // Apply/unmodify the debuff based on aggregate sustained contact.
        Set<Object> check = new HashSet<Object>(targetsHitThisFrame);
        check.addAll(beamDebuffTargets);
        for (Object tgt : check) {
            if (!(tgt instanceof ShipAPI)) continue;
            ShipAPI ship = (ShipAPI) tgt;
            if (beamTracker.hasDebuff(tgt)) {
                ship.getMutableStats().getFluxDissipation().modifyMult(BEAM_DEBUFF_ID, beamTracker.getDebuffMult(tgt));
                beamDebuffTargets.add(tgt);
            } else {
                ship.getMutableStats().getFluxDissipation().unmodify(BEAM_DEBUFF_ID);
                beamDebuffTargets.remove(tgt);
            }
        }
    }

    /** True while the Officer Imprint forge device's timed campaign flag is live. */
    private boolean isOfficerImprintActive() {
        try {
            if (Global.getSector() == null) return false;
            Object exp = Global.getSector().getMemoryWithoutUpdate().get(OFFICER_IMPRINT_EXPIRE_KEY);
            if (!(exp instanceof Number)) return false;
            int today = skills_overhaul.artificer.alphastorm.AlphaStormAbility.monotonicDay();
            return today < ((Number) exp).intValue();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Fighter wings launched from officer-piloted carriers gain damage & speed scaled by officer level. */
    private void applyOfficerImprint(CombatEngineAPI engine) {
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isFighter()) continue;
            FighterWingAPI wing = s.getWing();
            if (wing == null) continue;
            ShipAPI carrier = wing.getSourceShip();
            if (carrier == null) continue;
            PersonAPI captain = carrier.getCaptain();
            if (captain == null || captain.isDefault()) continue;
            int level = captain.getStats().getLevel();
            if (level <= 0) continue;
            float mult = 1f + Math.min(0.20f, 0.02f * level);
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult(OFFICER_IMPRINT_ID, mult);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult(OFFICER_IMPRINT_ID, mult);
            s.getMutableStats().getMaxSpeed().modifyMult(OFFICER_IMPRINT_ID, mult);
        }
    }

    private void applyPerShipEffects(ShipAPI s, CombatEngineAPI engine, float amount) {
        // T1
        if (ACTIVE_NODES.contains("artificer_t1_ecm")) ECM.apply(s, ecmRangePct);
        if (ACTIVE_NODES.contains("artificer_t1_ewm")) EnergyWeaponMastery.apply(s);
        if (ACTIVE_NODES.contains("artificer_t1_systems")) SystemsExpertise.apply(s);
        // T2
        if (ACTIVE_NODES.contains("artificer_t2_optics")) PrecisionOptics.apply(s);
        if (ACTIVE_NODES.contains("artificer_t2_strike")) StrikePackage.apply(s);
        if (ACTIVE_NODES.contains("artificer_t2_shield")) ShieldMastery.apply(s);
        if (ACTIVE_NODES.contains("artificer_t2_standard_systems")) StandardisedSystems.apply(s);
        // T3
        if (ACTIVE_NODES.contains("artificer_t3_beam_spec")) BeamSpec.apply(s);
        if (ACTIVE_NODES.contains("artificer_t3_adv_strike")) AdvStrike.apply(s);
        if (ACTIVE_NODES.contains("artificer_t3_resilience")) TechResilience.apply(s);
        if (ACTIVE_NODES.contains("artificer_t3_power_grid")) PowerGrid.apply(s, engine);
        // T4
        if (ACTIVE_NODES.contains("artificer_t4_transcendence")) TechTranscendence.apply(s, phaseBonus);
        if (ACTIVE_NODES.contains("artificer_t4_carrier_command")) CarrierCommand.apply(s);
        if (ACTIVE_NODES.contains("artificer_t4_limit_breaker")) LimitBreaker.apply(s);
    }

    // ---------- Effect inner classes ----------

    static final class ECM {
        /** Fleet-wide range bonus %, computed once per frame (two full-fleet scans). */
        static float computeRangePct() {
            float adv = ECMTracker.advantage(
                    ECMTracker.getPlayerECMRatingLive(),
                    ECMTracker.getEnemyECMRatingLive());
            float enemyRangePenalty = ECMTracker.enemyWeaponRangePenaltyMult(adv);
            return (1f - enemyRangePenalty) * 100f;
        }

        static void apply(ShipAPI s, float rangePct) {
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
            if (Misc.isAutomated(s)) {
                s.getMutableStats().getEmpDamageTakenMult().modifyMult("artificer_t2_strike", 0.80f);
                s.getMutableStats().getShieldDamageTakenMult().modifyMult("artificer_t2_strike", 0.90f);
            }
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
        /** Per-ship offensive buff. Sustained-contact debuff is handled fleet-wide in processBeamContact(). */
        static void apply(ShipAPI s) {
            s.getMutableStats().getBeamWeaponDamageMult().modifyMult("artificer_t3_beam_spec", 1.20f);
            float fluxFraction = s.getFluxLevel();
            float rofMult = fluxFraction > 0.5f ? 1.20f : 1.10f;
            s.getMutableStats().getEnergyRoFMult().modifyMult("artificer_t3_beam_spec", rofMult);
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
            if (Misc.isAutomated(s)) {
                s.getMutableStats().getHullDamageTakenMult().modifyMult("artificer_t3_resilience", 0.90f);
            }
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
        static void apply(ShipAPI s, float bonus) {
            if (s.getPhaseCloak() == null) return;
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
            if (!Misc.isAutomated(s)) return;
            s.getMutableStats().getBallisticWeaponDamageMult().modifyMult("artificer_t4_limit_breaker", 1.20f);
            s.getMutableStats().getEnergyWeaponDamageMult().modifyMult("artificer_t4_limit_breaker", 1.20f);
            s.getMutableStats().getMaxSpeed().modifyMult("artificer_t4_limit_breaker", 1.20f);
            s.getMutableStats().getArmorBonus().modifyMult("artificer_t4_limit_breaker", 1.20f);
        }
    }
}
