package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.Set;

public class TesseraMimicHullMod extends BaseHullMod {

    public static final String ID = "so_tessera_mimic";

    private static final Set<String> spawned = new HashSet<>();
    private static final String[] FALLBACK_VARIANTS = { "mining_drone", "borer_drone" };

    public static void clearSpawned() {
        spawned.clear();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (ship.isAlive()) return;

        String shipId = ship.getId();
        if (spawned.contains(shipId)) return;
        spawned.add(shipId);

        int count = 2 + (int) (Math.random() * 2);
        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.random() * 360);
            Vector2f pos = new Vector2f(
                    ship.getLocation().x + (float) Math.cos(Math.toRadians(angle)) * 120f,
                    ship.getLocation().y + (float) Math.sin(Math.toRadians(angle)) * 120f);
            spawnDrone(engine, ship.getOwner(), pos, angle);
        }
    }

    private static void spawnDrone(CombatEngineAPI engine, int owner, Vector2f pos, float angle) {
        for (String variantId : FALLBACK_VARIANTS) {
            try {
                FleetMemberAPI member = Global.getSettings().createFleetMember(
                        FleetMemberType.SHIP, variantId);
                engine.getFleetManager(owner).spawnFleetMember(member, pos, angle, 0f);
                return;
            } catch (Throwable t) {
                // try next variant
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}
