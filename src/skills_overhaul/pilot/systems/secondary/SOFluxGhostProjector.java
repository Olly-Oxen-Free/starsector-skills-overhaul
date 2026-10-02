package skills_overhaul.pilot.systems.secondary;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class SOFluxGhostProjector extends SOSecondarySystem {

    private static final float DURATION       = 20f;
    private static final float SPAWN_OFFSET   = 200f; // spawn slightly ahead of flagship
    private static final Color MSG_COLOR      = new Color(100, 200, 255, 255);

    // Ghost debuff values per spec: 30% weapon dmg, 1.5× dmg taken
    private static final float WEAPON_DMG_MULT    = 0.30f;
    private static final float DMG_TAKEN_MULT     = 1.50f;

    private ShipAPI ghost = null;

    @Override protected float baseCooldown()   { return 45f; }
    @Override protected float activeDuration() { return DURATION; }
    @Override public String getDisplayName()   { return "Flux Ghost Projector"; }

    @Override
    protected void onActivate(ShipAPI ship, CombatEngineAPI engine) {
        String variantId = ship.getVariant().getHullVariantId();
        if (variantId == null || variantId.isEmpty()) {
            engine.getCombatUI().addMessage(1, MSG_COLOR, "Ghost Projector: variant unavailable");
            return;
        }

        float rad = (float) Math.toRadians(ship.getFacing());
        Vector2f spawnPos = new Vector2f(
            ship.getLocation().x + (float) Math.cos(rad) * SPAWN_OFFSET,
            ship.getLocation().y + (float) Math.sin(rad) * SPAWN_OFFSET
        );

        try {
            // Clone the live variant: player refits carry custom variant ids that are
            // not registered globally, so createFleetMember(SHIP, variantId) throws.
            FleetMemberAPI ghostMember = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, ship.getVariant().clone());
            ghostMember.getRepairTracker().setCrashMothballed(false);
            ghostMember.getRepairTracker().setMothballed(false);
            ghostMember.getRepairTracker().setCR(ship.getCurrentCR() * 0.8f);
            ghostMember.setOwner(ship.getOwner());
            ghostMember.setAlly(ship.isAlly());
            ghostMember.setShipName("Flux Ghost");

            int owner = ship.getOwner();
            CombatFleetManagerAPI fm = engine.getFleetManager(owner);
            boolean suppress = fm.isSuppressDeploymentMessages();
            fm.setSuppressDeploymentMessages(true);

            ghost = fm.spawnFleetMember(ghostMember, spawnPos, ship.getFacing(), 0f);

            fm.setSuppressDeploymentMessages(suppress);

            if (ghost != null) {
                ghost.setInvalidTransferCommandTarget(true);
                ghost.getVelocity().set(ship.getVelocity());
                // Apply spec debuffs: 30% weapon damage, 1.5× damage taken
                ghost.getMutableStats().getBallisticWeaponDamageMult().modifyMult("so_ghost", WEAPON_DMG_MULT);
                ghost.getMutableStats().getEnergyWeaponDamageMult().modifyMult("so_ghost", WEAPON_DMG_MULT);
                ghost.getMutableStats().getMissileWeaponDamageMult().modifyMult("so_ghost", WEAPON_DMG_MULT);
                ghost.getMutableStats().getHullDamageTakenMult().modifyMult("so_ghost", DMG_TAKEN_MULT);
                ghost.getMutableStats().getShieldDamageTakenMult().modifyMult("so_ghost", DMG_TAKEN_MULT);
                engine.getCombatUI().addMessage(1, MSG_COLOR, "Flux Ghost projected — 20s duration");
            }
        } catch (Exception e) {
            Global.getLogger(SOFluxGhostProjector.class).warn("[SkillsOverhaul] SOFluxGhostProjector spawn failed: " + e.getMessage());
            engine.getCombatUI().addMessage(1, MSG_COLOR, "Ghost Projector: spawn failed");
        }
    }

    @Override
    public void advance(float dt, ShipAPI ship, CombatEngineAPI engine) {
        if (active) {
            activeTimer -= dt;
            if (ghost != null && !ghost.isAlive()) {
                // Ghost was destroyed early — end active state
                active = false;
                ghost = null;
            } else if (activeTimer <= 0f) {
                active = false;
                removeGhost(engine);
            }
        }
        tickCooldown(dt);
    }

    private void removeGhost(CombatEngineAPI engine) {
        if (ghost == null) return;
        if (ghost.isAlive()) engine.applyDamage(ghost, ghost.getLocation(), 9999999f, com.fs.starfarer.api.combat.DamageType.OTHER, 0f, true, false, null);
        ghost = null;
    }
}
