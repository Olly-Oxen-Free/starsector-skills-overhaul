package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public final class SOStasisEmitter extends SOSecondarySystem {
    private static final float DURATION = 6f;
    private static final float RADIUS = 700f;

    private final List<ShipAPI> affectedShips = new ArrayList<ShipAPI>();

    @Override protected float baseCooldown() { return 32f; }
    @Override protected float activeDuration() { return DURATION; }
    @Override public String getDisplayName() { return "Stasis Emitter"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        affectedShips.clear();
        for (ShipAPI s : engine.getShips()) {
            if (s == ship) continue;
            if (Misc.getDistance(ship.getLocation(), s.getLocation()) > RADIUS) continue;
            s.getMutableStats().getMaxSpeed().modifyMult("so_stasis", 0.60f);
            s.getMutableStats().getWeaponTurnRateBonus().modifyMult("so_stasis_wt", 0.80f);
            affectedShips.add(s);
        }
        ship.getMutableStats().getHullDamageTakenMult().modifyMult("so_stasis_dr", 0.75f);
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult("so_stasis_dr", 0.75f);
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (active) {
            activeTimer -= dt;
            if (activeTimer <= 0f) {
                active = false;
                for (ShipAPI s : affectedShips) {
                    s.getMutableStats().getMaxSpeed().unmodify("so_stasis");
                    s.getMutableStats().getWeaponTurnRateBonus().unmodifyMult("so_stasis_wt");
                }
                affectedShips.clear();
                ship.getMutableStats().getHullDamageTakenMult().unmodify("so_stasis_dr");
                ship.getMutableStats().getShieldDamageTakenMult().unmodify("so_stasis_dr");
            }
        }
        tickCooldown(dt);
    }
}
