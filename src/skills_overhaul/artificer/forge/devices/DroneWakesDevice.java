package skills_overhaul.artificer.forge.devices;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import skills_overhaul.artificer.forge.ForgeDeviceEffect;

import org.lwjgl.util.vector.Vector2f;

public final class DroneWakesDevice implements ForgeDeviceEffect {

    @Override
    public void deployBattle(CombatEngineAPI engine, boolean overQuota) {
        ShipAPI flagship = engine.getPlayerShip();
        if (flagship == null) return;
        boolean fourDrones = Boolean.TRUE.equals(
                Global.getSector().getMemoryWithoutUpdate().get(
                        "$skills.artificer.forge.drone_wakes_4"));
        int count = fourDrones ? 4 : 2;
        boolean combatDrones = Boolean.TRUE.equals(
                Global.getSector().getMemoryWithoutUpdate().get(
                        "$skills.artificer.forge.drone_wakes_combat"));
        String shipId = combatDrones ? "borer_drone" : "borer_drone";  // PD drone for v1; combat variant TBD

        for (int i = 0; i < count; i++) {
            float angle = (360f / count) * i;
            Vector2f offset = new Vector2f(
                    (float) Math.cos(Math.toRadians(angle)) * 200f,
                    (float) Math.sin(Math.toRadians(angle)) * 200f);
            Vector2f spawnPos = new Vector2f(flagship.getLocation().x + offset.x,
                    flagship.getLocation().y + offset.y);
            try {
                FleetMemberAPI member = Global.getSettings().createFleetMember(
                        FleetMemberType.SHIP, shipId);
                CombatFleetManagerAPI mgr = engine.getFleetManager(0);
                mgr.spawnFleetMember(member, spawnPos, angle, 1f);
            } catch (Throwable t) {
                Global.getLogger(DroneWakesDevice.class).warn(
                        "[DroneWakes] spawn failed: " + t.getMessage());
            }
        }
    }
}
