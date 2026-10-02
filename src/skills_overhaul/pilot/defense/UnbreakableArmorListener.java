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

    private static final float BLOCK_DURATION = 8f;
    private static final float COOLDOWN_DURATION = 15f;
    private static final float FLOOR = 0.50f;

    private final ShipAPI flagship;
    private final Map<String, CellState> cellStates = new HashMap<>();

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
        CellState state = cellStates.get(key);
        if (state == null) {
            state = new CellState();
            cellStates.put(key, state);
        }

        if (state.cooldownRemaining > 0f) return null;  // can't re-trigger

        float maxCell = grid.getMaxArmorInCell();
        float currCell = grid.getArmorValue(cell[0], cell[1]);
        float floorVal = maxCell * FLOOR;

        if (currCell <= floorVal) return null;  // already below floor — no protection

        // If incoming damage would drop the cell below floor, clamp it.
        float dmgValue = damage.getDamage();
        float projectedCell = currCell - dmgValue;
        if (projectedCell < floorVal) {
            float allowed = currCell - floorVal;
            // DamageAPI.getModifier() is a multiplier (base 1): a flat −(excess) drove
            // it negative on big hits. Scale the hit down to what the floor allows.
            damage.getModifier().modifyMult("so_unbreakable_clamp", Math.max(0f, allowed / dmgValue));
            state.startBlock();
            return "so_unbreakable";
        }
        return null;
    }

    /** Tick all per-cell timers; called from plugin advance(). */
    public void advance(float dt) {
        for (CellState s : cellStates.values()) s.advance(dt);
    }

    private static final class CellState {
        float blockRemaining = 0f;
        float cooldownRemaining = 0f;

        void startBlock() {
            blockRemaining = BLOCK_DURATION;
        }
        void advance(float dt) {
            if (blockRemaining > 0f) {
                blockRemaining = Math.max(0f, blockRemaining - dt);
                if (blockRemaining == 0f) cooldownRemaining = COOLDOWN_DURATION;
            } else if (cooldownRemaining > 0f) {
                cooldownRemaining = Math.max(0f, cooldownRemaining - dt);
            }
        }
    }
}
