package skills_overhaul.pilot.gunnery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GunneryCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<String>();

    private ShipAPI flagship;
    private LockOnState lockOn;
    private ThreatAcquisitionState threatAcq;
    private KillingMachineState killingMachine;
    private BountyHunterState bountyHunter;
    private float hitAccum = 0f;
    private final IntervalUtil wolfpackInterval = new IntervalUtil(0.5f, 0.5f);
    private boolean initialized = false;

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        ACTIVE_NODES.clear();
    }

    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (!initialized) initState(engine);
        if (flagship == null || !flagship.isAlive()) return;

        String targetId = flagship.getShipTarget() != null
                ? flagship.getShipTarget().getId() : null;

        if (ACTIVE_NODES.contains("so_gunnery_lock_on"))
            lockOn.advance(dt, targetId);

        if (ACTIVE_NODES.contains("so_gunnery_threat_acq")) {
            boolean firing = isFiringAtTarget(flagship);
            if (firing) {
                hitAccum += dt;
                while (hitAccum >= 1f) { threatAcq.onHittingTarget(1f); hitAccum -= 1f; }
            } else {
                hitAccum = 0f;
                threatAcq.advanceNoHit(dt);
            }
        }

        if (ACTIVE_NODES.contains("so_gunnery_bounty_hunter"))
            bountyHunter.advance(dt);

        wolfpackInterval.advance(dt);
        applyMods(engine);
    }

    private boolean isFiringAtTarget(ShipAPI ship) {
        if (ship.getShipTarget() == null) return false;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isFiring()) return true;
        }
        return false;
    }

    private void applyMods(CombatEngineAPI engine) {
        MutableShipStatsAPI stats = flagship.getMutableStats();

        // Lock On: +5% damage, -5% flux cost when locked
        if (ACTIVE_NODES.contains("so_gunnery_lock_on") && lockOn.isLocked()) {
            stats.getBallisticWeaponDamageMult().modifyMult("so_lock_on", 1.05f);
            stats.getEnergyWeaponDamageMult().modifyMult("so_lock_on", 1.05f);
            stats.getMissileWeaponDamageMult().modifyMult("so_lock_on", 1.05f);
            stats.getBallisticWeaponFluxCostMod().modifyMult("so_lock_on", 0.95f);
            stats.getEnergyWeaponFluxCostMod().modifyMult("so_lock_on", 0.95f);
        } else {
            stats.getBallisticWeaponDamageMult().unmodify("so_lock_on");
            stats.getEnergyWeaponDamageMult().unmodify("so_lock_on");
            stats.getMissileWeaponDamageMult().unmodify("so_lock_on");
            stats.getBallisticWeaponFluxCostMod().unmodify("so_lock_on");
            stats.getEnergyWeaponFluxCostMod().unmodify("so_lock_on");
        }

        // Threat Acquisition: +1% damage/DR per stack, max 20%
        if (ACTIVE_NODES.contains("so_gunnery_threat_acq")) {
            float bonus = threatAcq.getBonusFraction();
            stats.getBallisticWeaponDamageMult().modifyMult("so_threat_acq", 1f + bonus);
            stats.getEnergyWeaponDamageMult().modifyMult("so_threat_acq", 1f + bonus);
            stats.getMissileWeaponDamageMult().modifyMult("so_threat_acq", 1f + bonus);
            stats.getHullDamageTakenMult().modifyMult("so_threat_acq", 1f - bonus);
        }

        // Killing Machine: +3% damage per kill-stack, −2% glass cannon penalty per stack
        if (ACTIVE_NODES.contains("so_gunnery_killing_machine")) {
            float dmg = killingMachine.getDamageBonus();
            stats.getBallisticWeaponDamageMult().modifyMult("so_killing_machine", 1f + dmg);
            stats.getEnergyWeaponDamageMult().modifyMult("so_killing_machine", 1f + dmg);
            stats.getMissileWeaponDamageMult().modifyMult("so_killing_machine", 1f + dmg);
            if (ACTIVE_NODES.contains("so_gunnery_glass_cannon")) {
                float relief = killingMachine.getGlassCannonPenaltyRelief();
                stats.getHullDamageTakenMult().modifyMult("so_glass_cannon_relief", 1f - relief);
            }
        }

        // Bounty Hunter: +20% flux dissipation for 8s after kill
        if (ACTIVE_NODES.contains("so_gunnery_bounty_hunter") && bountyHunter.isFluxBonusActive()) {
            stats.getFluxDissipation().modifyMult("so_bounty_hunter", 1.20f);
        } else {
            stats.getFluxDissipation().unmodify("so_bounty_hunter");
        }

        // Berserker: damage scales with flux level
        if (ACTIVE_NODES.contains("so_gunnery_berserker")) {
            float maxFlux = flagship.getFluxTracker().getMaxFlux();
            float fluxFrac = maxFlux > 0f ? flagship.getFluxTracker().getCurrFlux() / maxFlux : 0f;
            float bonus = fluxFrac * 0.20f;
            stats.getBallisticWeaponDamageMult().modifyMult("so_berserker", 1f + bonus);
            stats.getEnergyWeaponDamageMult().modifyMult("so_berserker", 1f + bonus);
            stats.getMissileWeaponDamageMult().modifyMult("so_berserker", 1f + bonus);
        }

        // Overload Hunter: +25% damage while target is overloaded
        if (ACTIVE_NODES.contains("so_gunnery_overload_hunter")) {
            ShipAPI target = flagship.getShipTarget();
            if (target != null && target.getFluxTracker().isOverloaded()) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_overload_hunter", 1.25f);
                stats.getEnergyWeaponDamageMult().modifyMult("so_overload_hunter", 1.25f);
                stats.getMissileWeaponDamageMult().modifyMult("so_overload_hunter", 1.25f);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_overload_hunter");
                stats.getEnergyWeaponDamageMult().unmodify("so_overload_hunter");
                stats.getMissileWeaponDamageMult().unmodify("so_overload_hunter");
            }
        }

        // Dragon Slayer: bonus vs larger hull sizes
        if (ACTIVE_NODES.contains("so_gunnery_dragon_slayer") && flagship.getShipTarget() != null) {
            int diff = flagship.getShipTarget().getHullSize().ordinal() - flagship.getHullSize().ordinal();
            float bonus = Math.min(0.30f, Math.max(0f, diff * 0.10f));
            if (bonus > 0f) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_dragon_slayer", 1f + bonus);
                stats.getEnergyWeaponDamageMult().modifyMult("so_dragon_slayer", 1f + bonus);
                stats.getMissileWeaponDamageMult().modifyMult("so_dragon_slayer", 1f + bonus);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_dragon_slayer");
                stats.getEnergyWeaponDamageMult().unmodify("so_dragon_slayer");
                stats.getMissileWeaponDamageMult().unmodify("so_dragon_slayer");
            }
        }

        // Point Blank: within 400u → +20% damage, +50% weapon turn rate
        if (ACTIVE_NODES.contains("so_gunnery_point_blank") && flagship.getShipTarget() != null) {
            float dist = com.fs.starfarer.api.util.Misc.getDistance(
                    flagship.getLocation(), flagship.getShipTarget().getLocation());
            if (dist <= 400f) {
                stats.getBallisticWeaponDamageMult().modifyMult("so_point_blank", 1.20f);
                stats.getEnergyWeaponDamageMult().modifyMult("so_point_blank", 1.20f);
                stats.getMissileWeaponDamageMult().modifyMult("so_point_blank", 1.20f);
                stats.getWeaponTurnRateBonus().modifyPercent("so_point_blank", 50f);
            } else {
                stats.getBallisticWeaponDamageMult().unmodify("so_point_blank");
                stats.getEnergyWeaponDamageMult().unmodify("so_point_blank");
                stats.getMissileWeaponDamageMult().unmodify("so_point_blank");
                stats.getWeaponTurnRateBonus().unmodifyPercent("so_point_blank");
            }
        }

        // Wolfpack: +5% per nearby ally, +1% per nearby fighter (polled every 0.5s)
        if (ACTIVE_NODES.contains("so_gunnery_wolfpack") && wolfpackInterval.intervalElapsed()) {
            int allies = 0, fighters = 0;
            for (ShipAPI s : engine.getShips()) {
                if (s == flagship || s.getOwner() != 0) continue;
                float dist = com.fs.starfarer.api.util.Misc.getDistance(flagship.getLocation(), s.getLocation());
                if (dist > 1750f) continue;
                if (s.getHullSize() == ShipAPI.HullSize.FIGHTER) fighters++; else allies++;
            }
            float bonus = allies * 0.05f + fighters * 0.01f;
            stats.getBallisticWeaponDamageMult().modifyMult("so_wolfpack", 1f + bonus);
            stats.getEnergyWeaponDamageMult().modifyMult("so_wolfpack", 1f + bonus);
            stats.getMissileWeaponDamageMult().modifyMult("so_wolfpack", 1f + bonus);
        }
    }

    public void onKill(ShipAPI killed, float playerContribution) {
        if (playerContribution < 0.15f) return;
        if (ACTIVE_NODES.contains("so_gunnery_threat_acq")) threatAcq.onKill();
        if (ACTIVE_NODES.contains("so_gunnery_killing_machine")) killingMachine.onKill();
        if (ACTIVE_NODES.contains("so_gunnery_bounty_hunter")) bountyHunter.onKill();
    }

    private void initState(CombatEngineAPI engine) {
        flagship = engine.getPlayerShip();
        lockOn = new LockOnState();
        threatAcq = new ThreatAcquisitionState();
        killingMachine = new KillingMachineState();
        bountyHunter = new BountyHunterState();
        initialized = true;
    }
}
