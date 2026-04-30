package skills_overhaul.collective;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SOTerminationProtocolLogic {

    private static final String SRC            = "so_termination";
    private static final float  INVULN_SECS    = 1.5f;
    private static final float  HULL_THRESHOLD = 0.10f;

    private final Set<String>         triggered   = new HashSet<String>();
    private final Map<String, Float>  invulnTimer = new HashMap<String, Float>();

    public void advance(CombatEngineAPI engine, float amount) {
        if (!CollectiveActiveNodes.isActive("so_collective_termination_protocol")) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive()) continue;
            if (!isAICored(ship)) continue;

            String id = ship.getId();
            MutableShipStatsAPI stats = ship.getMutableStats();

            // Trigger on first time hull drops below threshold
            if (!triggered.contains(id) && ship.getHullLevel() < HULL_THRESHOLD) {
                triggered.add(id);
                invulnTimer.put(id, 0f);
                stats.getArmorDamageTakenMult().modifyMult(SRC, 0f);
                stats.getHullDamageTakenMult().modifyMult(SRC, 0f);
            }

            // Tick invuln timer
            if (invulnTimer.containsKey(id)) {
                float elapsed = invulnTimer.get(id) + amount;
                if (elapsed >= INVULN_SECS) {
                    // Remove invuln and self-destruct
                    stats.getArmorDamageTakenMult().unmodify(SRC);
                    stats.getHullDamageTakenMult().unmodify(SRC);
                    invulnTimer.remove(id);
                    engine.applyDamage(ship, ship.getLocation(),
                            999_999f, DamageType.ENERGY, 0f, true, false, ship);
                } else {
                    invulnTimer.put(id, elapsed);
                }
            }
        }
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }
}
