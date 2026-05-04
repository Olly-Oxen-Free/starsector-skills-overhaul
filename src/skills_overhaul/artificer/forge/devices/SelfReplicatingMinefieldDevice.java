package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public final class SelfReplicatingMinefieldDevice implements ForgeDeviceEffect {

    public static final int INITIAL_MINES = 8;
    public static final float SPAWN_RADIUS = 1500f;
    public static final String MINE_HULL_ID = "mine_strike_drone";
    private static final float DURATION = 60f;
    private static final float REPLICATION_INTERVAL = 5f;

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
            spawnMine(engine, pos, angle);
        }
        engine.addPlugin(new MineReplicationTracker(flagship));
    }

    static void spawnMine(CombatEngineAPI engine, Vector2f pos, float angle) {
        try {
            FleetMemberAPI member = Global.getSettings().createFleetMember(
                    FleetMemberType.SHIP, MINE_HULL_ID);
            engine.getFleetManager(0).spawnFleetMember(member, pos, angle, 0f);
        } catch (Throwable t) {
            Global.getLogger(SelfReplicatingMinefieldDevice.class).warn(
                    "[Minefield] spawn failed: " + t.getMessage());
        }
    }

    static int countLiveMines(CombatEngineAPI engine) {
        int count = 0;
        for (ShipAPI s : engine.getShips()) {
            if (s.isAlive() && MINE_HULL_ID.equals(s.getHullSpec().getHullId())) count++;
        }
        return count;
    }

    static final class MineReplicationTracker extends BaseEveryFrameCombatPlugin {
        private final ShipAPI flagship;
        private float elapsed = 0f;
        private float nextReplication = REPLICATION_INTERVAL;

        MineReplicationTracker(ShipAPI flagship) {
            this.flagship = flagship;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            elapsed += amount;
            if (elapsed >= DURATION) {
                engine.removePlugin(this);
                return;
            }
            nextReplication -= amount;
            if (nextReplication <= 0f) {
                nextReplication = REPLICATION_INTERVAL;
                int live = countLiveMines(engine);
                if (live < INITIAL_MINES && flagship != null && flagship.isAlive()) {
                    float angle = (float) (Math.random() * 360);
                    float r = SPAWN_RADIUS * (0.4f + (float) Math.random() * 0.6f);
                    Vector2f pos = new Vector2f(
                            flagship.getLocation().x + r * (float) Math.cos(Math.toRadians(angle)),
                            flagship.getLocation().y + r * (float) Math.sin(Math.toRadians(angle)));
                    spawnMine(engine, pos, angle);
                }
            }
        }
    }
}
