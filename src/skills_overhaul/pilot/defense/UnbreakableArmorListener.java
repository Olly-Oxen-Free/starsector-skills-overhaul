package skills_overhaul.pilot.defense;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

/**
 * Unbreakable (T4) — per-cell armor floor 20%→50%.
 *
 * Spec line 57: armor location floor raised to 50%, 8s block + 15s cooldown
 * per location. When a cell at or below 50% takes a hit, clamp damage to
 * stop at 50% (block phase, 8s). After block phase ends, start 15s cooldown
 * before that cell can trigger again.
 *
 * Per-cell state stored in a Map keyed by armor grid coords.
 */
public final class UnbreakableArmorListener implements DamageTakenModifier {

    private final ShipAPI flagship;
    private final Map<String, UnbreakableCellState> cellStates = new HashMap<>();

    public UnbreakableArmorListener(ShipAPI flagship) { this.flagship = flagship; }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target,
                                     DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target != flagship || shieldHit) return null;
        if (damage == null || point == null) return null;

        ArmorGridAPI grid = flagship.getArmorGrid();
        if (grid == null) return null;
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return null;

        String key = cell[0] + "," + cell[1];
        UnbreakableCellState state = cellStates.get(key);
        if (state == null) {
            state = new UnbreakableCellState();
            cellStates.put(key, state);
        }

        float mult = state.onHit(grid.getArmorValue(cell[0], cell[1]), grid.getMaxArmorInCell(),
                damage.getDamage());
        if (mult < 1f) {
            damage.getModifier().modifyMult("so_unbreakable_clamp", mult);
            return "so_unbreakable";
        }
        return null;
    }

    /** Tick all per-cell timers; called from plugin advance(). */
    public void advance(float dt) {
        for (UnbreakableCellState s : cellStates.values()) s.advance(dt);
    }
}
