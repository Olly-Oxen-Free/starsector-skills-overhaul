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

        if (ACTIVE_NODES.contains("so_defense_targe") && hullDelta > 0f) {
            targe.recordHullLost(hullDelta / maxHull);
            float regen = targe.getRegenPerSecondFraction() * maxHull * dt;
            flagship.setHitpoints(Math.min(maxHull, flagship.getHitpoints() + regen));
        }

        if (ACTIVE_NODES.contains("so_defense_deaths_door")) {
            deathsDoor.tryTrigger(currHull / maxHull);
            deathsDoor.advance(dt);
        }

        if (ACTIVE_NODES.contains("so_defense_adamantine")) {
            adamantine.updateFlux(flagship.getFluxTracker().getCurrFlux());
            if (hullDelta > 0f && adamantine.absorbHullDamage(hullDelta)) {
                engine.spawnExplosion(flagship.getLocation(), flagship.getVelocity(),
                        new java.awt.Color(100, 150, 255, 200), 300f, 1f);
            }
            adamantine.advance(dt);
        }

        if (ACTIVE_NODES.contains("so_defense_iron_bastion")) {
            if (hullDelta > 0f) ironBastion.onHullOrArmorDamage();
            ironBastion.advance(dt);
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
    }
}
