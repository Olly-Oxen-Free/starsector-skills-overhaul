package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class SODomainOverclock extends SOSecondarySystem {
    private static final float DURATION = 4f;
    private static final float TIMESCALE_MULT = 1.60f;

    @Override protected float baseCooldown() { return 20f; }
    @Override protected float activeDuration() { return DURATION; }
    @Override public String getDisplayName() { return "Domain Overclock"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {}

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (active) {
            engine.getTimeMult().modifyMult("so_domain_overclock", TIMESCALE_MULT);
            activeTimer -= dt;
            if (activeTimer <= 0f) {
                active = false;
                engine.getTimeMult().unmodify("so_domain_overclock");
            }
        }
        tickCooldown(dt);
    }
}
