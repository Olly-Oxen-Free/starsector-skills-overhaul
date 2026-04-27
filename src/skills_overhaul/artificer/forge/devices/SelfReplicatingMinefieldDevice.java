package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;

public final class SelfReplicatingMinefieldDevice implements ForgeDeviceEffect {

    public static final int INITIAL_MINES = 8;
    public static final float SPAWN_RADIUS = 1500f;
    public static final String MINE_HULL_ID = "mine_strike_drone";  // verify vanilla id

    @Override
    public void deployBattle(CombatEngineAPI engine, boolean overQuota) {
        ShipAPI flagship = engine.getPlayerShip();
        if (flagship == null) return;
        Vector2f center = flagship.getLocation();
        for (int i = 0; i < INITIAL_MINES; i++) {
            float angle = (360f / INITIAL_MINES) * i;
            float r = SPAWN_RADIUS * (0.5f + (float) Math.random() * 0.5f);
            Vector2f pos = new Vector2f(center.x + r * (float) Math.cos(Math.toRadians(angle)),
                    center.y + r * (float) Math.sin(Math.toRadians(angle)));
            try {
                FleetMemberAPI member = Global.getSettings().createFleetMember(
                        FleetMemberType.SHIP, MINE_HULL_ID);
                engine.getFleetManager(0).spawnFleetMember(member, pos, angle, 0f);
            } catch (Throwable t) {
                Global.getLogger(SelfReplicatingMinefieldDevice.class).warn(
                        "[Minefield] spawn failed: " + t.getMessage());
            }
        }
        // Replication tracker: per-frame plugin watches mine deaths and re-spawns up to cap.
        // v1: simplified — no replication. TODO: add MineReplicationTracker as follow-up.
    }
}
