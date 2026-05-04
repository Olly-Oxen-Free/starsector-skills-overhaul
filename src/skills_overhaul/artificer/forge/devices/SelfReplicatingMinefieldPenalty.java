package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import skills_overhaul.artificer.forge.ForgeDevicePenalty;

public final class SelfReplicatingMinefieldPenalty implements ForgeDevicePenalty {
    @Override
    public void apply(CombatEngineAPI engine, ShipAPI flagship) {
        // Over-quota penalty: mines hit friendlies. Set all mine ships to SHIP collision
        // so they collide with every faction including the player's own.
        for (ShipAPI s : engine.getShips()) {
            if (!s.isAlive()) continue;
            if (SelfReplicatingMinefieldDevice.MINE_HULL_ID.equals(s.getHullSpec().getHullId())) {
                s.setCollisionClass(CollisionClass.SHIP);
            }
        }
    }
}
