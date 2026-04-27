package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;

public final class DerelictDroneSpawnerDevice implements ForgeDeviceEffect {

    public static final int DRONE_COUNT = 4;
    public static final String DRONE_HULL_ID = "tesseract_small";  // verify vanilla automated drone

    @Override
    public void deployBattle(CombatEngineAPI engine, boolean overQuota) {
        ShipAPI flagship = engine.getPlayerShip();
        if (flagship == null) return;
        Vector2f anchor = flagship.getLocation();
        for (int i = 0; i < DRONE_COUNT; i++) {
            float angle = i * (360f / DRONE_COUNT);
            Vector2f pos = new Vector2f(anchor.x + 400f * (float) Math.cos(Math.toRadians(angle)),
                    anchor.y + 400f * (float) Math.sin(Math.toRadians(angle)));
            try {
                FleetMemberAPI member = Global.getSettings().createFleetMember(
                        FleetMemberType.SHIP, DRONE_HULL_ID);
                engine.getFleetManager(0).spawnFleetMember(member, pos, angle, 1f);
            } catch (Throwable t) {
                Global.getLogger(DerelictDroneSpawnerDevice.class).warn(
                        "[DerelictDroneSpawner] spawn failed: " + t.getMessage());
            }
        }
    }
}
