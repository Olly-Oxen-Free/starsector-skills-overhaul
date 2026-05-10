package skills_overhaul.pilot.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DefenseCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final Set<String> ACTIVE_NODES = new LinkedHashSet<String>();

    private ShipAPI flagship;
    private FortifyState fortify;
    private TargeState targe;
    private DeathsDoorState deathsDoor;
    private AdamantineState adamantine;
    private IronBastionState ironBastion;
    private float prevHull = -1f;
    private boolean initialized = false;
    private final java.util.Map<ShipAPI, Boolean> fighterAliveMap = new java.util.WeakHashMap<>();
    private final com.fs.starfarer.api.util.IntervalUtil targeRegenInterval =
            new com.fs.starfarer.api.util.IntervalUtil(0.5f, 0.5f);
    private float targeRegenAccumulator = 0f;

    @Override
    public void init(CombatEngineAPI engine) {
        initialized = false;
        ACTIVE_NODES.clear();
    }

    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        if (!initialized) {
            flagship = engine.getPlayerShip();
            if (flagship == null) return;
            fortify     = new FortifyState();
            targe       = new TargeState();
            deathsDoor  = new DeathsDoorState();
            adamantine  = new AdamantineState(flagship.getFluxTracker().getMaxFlux());
            ironBastion = new IronBastionState();
            if (ACTIVE_NODES.contains("so_defense_lead_front"))
                flagship.addListener(new LeadFromTheFrontListener(flagship));
            prevHull = flagship.getHitpoints();
            initialized = true;
        }

        float currHull = flagship.getHitpoints();
        float maxHull  = flagship.getMaxHitpoints();
        float hullDelta = Math.max(0f, prevHull - currHull);

        if (ACTIVE_NODES.contains("so_defense_fortify") && hullDelta > 100f)
            fortify.onQualifyingHit();
        fortify.advance(dt);

        if (ACTIVE_NODES.contains("so_defense_targe")) {
            if (hullDelta > 0f) targe.recordHullLost(hullDelta / maxHull);
            // Tick-based regen (spec §5.2 line 92): accumulate, apply once per 0.5s
            // to avoid fighting vanilla repair tracker (was per-frame setHitpoints).
            targeRegenAccumulator += targe.getRegenPerSecondFraction() * maxHull * dt;
            targeRegenInterval.advance(dt);
            if (targeRegenInterval.intervalElapsed() && targeRegenAccumulator > 0f) {
                flagship.setHitpoints(Math.min(maxHull, flagship.getHitpoints() + targeRegenAccumulator));
                targeRegenAccumulator = 0f;
            }
        }

        if (ACTIVE_NODES.contains("so_defense_deaths_door")) {
            deathsDoor.tryTrigger(currHull / maxHull);
            deathsDoor.advance(dt);
        }

        if (ACTIVE_NODES.contains("so_defense_adamantine")) {
            adamantine.updateFlux(flagship.getFluxTracker().getCurrFlux());
            if (hullDelta > 0f && adamantine.absorbHullDamage(hullDelta)) {
                engine.spawnExplosion(flagship.getLocation(), flagship.getVelocity(),
                        new java.awt.Color(100, 150, 255, 200), 600f, 1f);
                engine.spawnEmpArc(flagship, flagship.getLocation(), flagship, flagship,
                        com.fs.starfarer.api.combat.DamageType.ENERGY, 300f, 1500f, 600f,
                        "tachyon_lance_emp_impact", 20f,
                        new java.awt.Color(180, 220, 255, 220),
                        new java.awt.Color(255, 255, 255, 255));
                adamantine.startPostDischarge();
            }
            adamantine.advance(dt);
        }

        if (ACTIVE_NODES.contains("so_defense_iron_bastion")) {
            if (hullDelta > 0f) ironBastion.onHullOrArmorDamage();
            ironBastion.advance(dt);
            // Fighter-kill heal (spec §5.5 line 120): no ShipDestroyedListener in
            // 0.98a-RC8; poll for alive→dead transitions on owned fighters.
            for (ShipAPI s : engine.getShips()) {
                if (s.getHullSize() != ShipAPI.HullSize.FIGHTER) continue;
                boolean wasAlive = Boolean.TRUE.equals(fighterAliveMap.get(s));
                boolean isAlive = s.isAlive();
                fighterAliveMap.put(s, isAlive);
                if (wasAlive && !isAlive && s.getOwner() != flagship.getOwner()) {
                    float heal = ironBastion.computeFighterKillHeal(s.getMaxHitpoints());
                    flagship.setHitpoints(Math.min(flagship.getMaxHitpoints(),
                            flagship.getHitpoints() + heal));
                }
            }
        }

        applyMods();
        prevHull = flagship.getHitpoints();
    }

    private void applyMods() {
        if (flagship == null) return;
        MutableShipStatsAPI stats = flagship.getMutableStats();

        if (ACTIVE_NODES.contains("so_defense_fortify")) {
            float drMult = fortify.getDRMult();
            stats.getHullDamageTakenMult().modifyMult("so_fortify", drMult);
            stats.getArmorDamageTakenMult().modifyMult("so_fortify", drMult);
            // Spec §5.1 line 81: shield mult also part of Fortify (research caught).
            stats.getShieldDamageTakenMult().modifyMult("so_fortify", drMult);
        }

        // Shield Mastery (spec line 54): hard→soft flux conversion 8%/s while shielded.
        if (ACTIVE_NODES.contains("so_defense_shield_mastery")
                && flagship.getShield() != null && flagship.getShield().isOn()) {
            com.fs.starfarer.api.combat.FluxTrackerAPI flux = flagship.getFluxTracker();
            float dt = Global.getCombatEngine().getElapsedInLastFrame();
            float convertAmount = flux.getHardFlux() * 0.08f * dt;
            if (convertAmount > 0f) {
                float newHard = Math.max(0f, flux.getHardFlux() - convertAmount);
                flux.setHardFlux(newHard);
                // Soft flux fills in as hard drops; engine recomputes total automatically.
            }
        }

        if (ACTIVE_NODES.contains("so_defense_targe")) {
            float dmgMult = targe.getWeaponDamageMult();
            stats.getBallisticWeaponDamageMult().modifyMult("so_targe", dmgMult);
            stats.getEnergyWeaponDamageMult().modifyMult("so_targe", dmgMult);
            stats.getMissileWeaponDamageMult().modifyMult("so_targe", dmgMult);
        }

        if (ACTIVE_NODES.contains("so_defense_deaths_door") && deathsDoor.isImmune()) {
            stats.getHullDamageTakenMult().modifyMult("so_deaths_door", 0f);
        } else {
            stats.getHullDamageTakenMult().unmodify("so_deaths_door");
        }

        // Iron Bastion (spec §5.5 line 118): PD +20% fire rate + 25% turn rate
        // while takingActiveDamage. No PD-specific RoF stat exists; apply
        // universal RoF + global weapon turn rate (deviation documented).
        if (ACTIVE_NODES.contains("so_defense_iron_bastion") && ironBastion.isTakingActiveDamage()) {
            stats.getBallisticRoFMult().modifyMult("so_iron_bastion", 1.20f);
            stats.getEnergyRoFMult().modifyMult("so_iron_bastion", 1.20f);
            stats.getMissileRoFMult().modifyMult("so_iron_bastion", 1.20f);
            stats.getWeaponTurnRateBonus().modifyPercent("so_iron_bastion", 25f);
        } else {
            stats.getBallisticRoFMult().unmodify("so_iron_bastion");
            stats.getEnergyRoFMult().unmodify("so_iron_bastion");
            stats.getMissileRoFMult().unmodify("so_iron_bastion");
            stats.getWeaponTurnRateBonus().unmodify("so_iron_bastion");
        }

        // Flux Fortress (spec line 58): shield efficiency scales with flux
        // level (0 → +30% at 100% flux). Linear interpolation.
        if (ACTIVE_NODES.contains("so_defense_flux_fortress")) {
            float fluxFrac = flagship.getFluxTracker().getFluxLevel();
            float effMult = 1f - 0.30f * fluxFrac;  // efficiency mult <1 = better
            stats.getShieldDamageTakenMult().modifyMult("so_flux_fortress", effMult);
        } else {
            stats.getShieldDamageTakenMult().unmodify("so_flux_fortress");
        }

        // Overwhelming Force (spec line 61): gated on advancing condition.
        // +25% flux dissipation, +20% hard-flux dissipation while shields raised.
        if (ACTIVE_NODES.contains("so_defense_overwhelming_force")
                && flagship.getEngineController() != null
                && flagship.getEngineController().isAccelerating()) {
            stats.getFluxDissipation().modifyMult("so_overwhelming_force", 1.25f);
            boolean shieldUp = flagship.getShield() != null
                    && flagship.getShield().isOn();
            if (shieldUp) {
                stats.getHardFluxDissipationFraction().modifyFlat("so_overwhelming_force", 0.20f);
            } else {
                stats.getHardFluxDissipationFraction().unmodify("so_overwhelming_force");
            }
        } else {
            stats.getFluxDissipation().unmodify("so_overwhelming_force");
            stats.getHardFluxDissipationFraction().unmodify("so_overwhelming_force");
        }

        // Adamantine (spec §5.4 line 110): on discharge, apply −25% EMP taken
        // + +8% hull regen for post-discharge window (5s, matches CD).
        if (ACTIVE_NODES.contains("so_defense_adamantine") && adamantine.isPostDischargeActive()) {
            stats.getEmpDamageTakenMult().modifyMult("so_adamantine_post", 0.75f);
            float maxHull = flagship.getMaxHitpoints();
            float regen = 0.08f * maxHull / 5f;  // +8% hull spread over 5s window
            float curr = flagship.getHitpoints();
            float dt = Global.getCombatEngine().getElapsedInLastFrame();
            flagship.setHitpoints(Math.min(maxHull, curr + regen * dt));
        } else {
            stats.getEmpDamageTakenMult().unmodify("so_adamantine_post");
        }
    }
}
