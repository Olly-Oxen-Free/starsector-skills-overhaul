package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 * v1 stub: ship spawning in combat is not natively supported by the Starsector API.
 * Logs a UI message. Full impl deferred to when a drone/fighter-wing spawn path is confirmed.
 */
public final class SOFluxGhostProjector extends SOSecondarySystem {
    @Override protected float baseCooldown() { return 45f; }
    @Override protected float activeDuration() { return 0f; }
    @Override public String getDisplayName() { return "Flux Ghost Projector"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        engine.getCombatUI().addMessage(1, com.fs.starfarer.api.util.Misc.getTextColor(),
            "Ghost Projector: v1 stub - spawn API path TBD");
        Global.getLogger(SOFluxGhostProjector.class).info("[SkillsOverhaul] SOFluxGhostProjector activated (stub)");
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        tickCooldown(dt);
    }
}
