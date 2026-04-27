package skills_overhaul.artificer.alphastorm;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.Set;

public final class StormShieldObjective {

    public static final float RADIUS = 1500f;

    private final Vector2f location;
    private final Set<ShipAPI> sheltered = new HashSet<>();

    public StormShieldObjective(Vector2f location) { this.location = location; }

    public Vector2f getLocation() { return location; }

    public void tick(CombatEngineAPI engine) {
        sheltered.clear();
        for (ShipAPI s : engine.getShips()) {
            if (!s.isAlive()) continue;
            if (Misc.getDistance(location, s.getLocation()) <= RADIUS) {
                sheltered.add(s);
            }
        }
    }

    public boolean isSheltered(ShipAPI s) { return sheltered.contains(s); }
}
