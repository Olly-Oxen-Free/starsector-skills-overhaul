package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class SODomainOverclock extends SOSecondarySystem {
    private static final float DURATION = 4f;
    private static final float TIMESCALE_MULT = 1.60f;
    public static final String ID = "so_domain_overclock";

    @Override protected float baseCooldown() { return 20f; }
    @Override protected float activeDuration() { return DURATION; }
    @Override public String getDisplayName() { return "Domain Overclock"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {}

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (active) {
            // Local time only (vanilla TemporalShellStats pattern): speed up the ship,
            // and for the player's ship slow the engine clock to match. Modifying only
            // engine.getTimeMult() sped up the whole battle, enemies included.
            ship.getMutableStats().getTimeMult().modifyMult(ID, TIMESCALE_MULT);
            if (ship == engine.getPlayerShip()) {
                engine.getTimeMult().modifyMult(ID, 1f / TIMESCALE_MULT);
            } else {
                engine.getTimeMult().unmodify(ID);
            }
            activeTimer -= dt;
            if (activeTimer <= 0f) {
                active = false;
                ship.getMutableStats().getTimeMult().unmodify(ID);
                engine.getTimeMult().unmodify(ID);
            }
        }
        tickCooldown(dt);
    }
}
