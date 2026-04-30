package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Transspatial Interface — combat ship system script.
 *
 * Activation effects:
 *   - Allied AI-cored target: transfer player control to that ship.
 *   - Hostile target with ≤45 DP: override owner to player side for 30 seconds, then revert.
 *
 * Uses are initialized from AssimilationRegistry alpha count via {@link #initializeUses(MemoryAPI)}.
 * The system is attached to a ship hullspec in a future integration pass (Plan F).
 */
public final class TransspatialInterfaceSystem extends BaseShipSystemScript {

    private static final String USES_KEY          = "$so_collective.interface_uses_remaining";
    private static final float  MAX_HOSTILE_DP    = 45f;
    private static final float  OVERRIDE_DURATION = 30f;

    /** Tracks remaining time of hostile-override per ship id. */
    private final Map<String, Float> overrideTimers = new HashMap<String, Float>();
    /** Tracks original owner for ships under override. */
    private final Map<String, Integer> originalOwner = new HashMap<String, Integer>();

    private boolean activated = false;

    // ── Public static init ────────────────────────────────────────────────────

    /**
     * Called from CollectiveCombatPlugin.init() when the capstone node is active.
     * Writes the available uses into campaign memory for the system script to read.
     */
    public static void initializeUses(MemoryAPI mem) {
        MemoryView view = new MemoryAPIView(mem);
        int alphaCount = AssimilationRegistry.getAlphaCount(view);
        int uses = CollectiveLevel.getInterfaceUses(CollectiveLevel.fromAlphaCount(alphaCount));
        mem.set(USES_KEY, (float) uses, 0f);
    }

    // ── BaseShipSystemScript ──────────────────────────────────────────────────

    @Override
    public void advance(float amount, CombatEngineAPI engine, ShipAPI ship,
                        ShipSystemAPI system, MutableShipStatsAPI stats) {
        if (engine == null || engine.isPaused()) return;

        // Tick override timers each frame
        tickOverrideTimers(engine, amount);

        // Detect activation edge: effectLevel reaches full on the first qualifying frame
        boolean systemActive = system.isActive() || system.getEffectLevel() >= 0.99f;
        if (systemActive && !activated) {
            activated = true;
            onActivate(engine, ship);
        } else if (!systemActive) {
            activated = false;
        }
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (getRemainingUses() <= 0) return false;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return false;
        ShipAPI target = ship.getShipTarget();
        if (target == null) return false;
        return isAlliedAICored(target) || isHostileLowDP(target);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void onActivate(CombatEngineAPI engine, ShipAPI ship) {
        ShipAPI target = ship.getShipTarget();
        if (target == null || !target.isAlive()) return;

        if (isAlliedAICored(target)) {
            decrementUses();
            // Transfer player control to the allied AI-cored ship
            engine.setPlayerShip(target);
        } else if (isHostileLowDP(target)) {
            decrementUses();
            // Override ownership to player side for OVERRIDE_DURATION seconds
            String id = target.getId();
            if (!overrideTimers.containsKey(id)) {
                originalOwner.put(id, target.getOwner());
            }
            target.setOwner(0);
            overrideTimers.put(id, OVERRIDE_DURATION);
        }
    }

    private void tickOverrideTimers(CombatEngineAPI engine, float amount) {
        if (overrideTimers.isEmpty()) return;
        java.util.Iterator<Map.Entry<String, Float>> it = overrideTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> entry = it.next();
            float remaining = entry.getValue() - amount;
            if (remaining <= 0f) {
                it.remove();
                // Revert ownership
                String id = entry.getKey();
                Integer orig = originalOwner.remove(id);
                if (orig != null) {
                    for (ShipAPI s : engine.getShips()) {
                        if (s.getId().equals(id)) {
                            s.setOwner(orig);
                            break;
                        }
                    }
                }
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private static boolean isAlliedAICored(ShipAPI target) {
        return target.getOwner() == 0
                && target.getCaptain() != null
                && target.getCaptain().isAICore();
    }

    private static boolean isHostileLowDP(ShipAPI target) {
        if (target.getOwner() != 1) return false;
        float dp = 0f;
        try {
            if (target.getFleetMember() != null) {
                dp = target.getFleetMember().getDeploymentPointsCost();
            }
        } catch (Exception ignored) {}
        return dp <= MAX_HOSTILE_DP;
    }

    private int getRemainingUses() {
        try {
            MemoryAPI mem = Global.getSector().getCharacterData().getMemory();
            Object v = mem.get(USES_KEY);
            return v instanceof Number ? ((Number) v).intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void decrementUses() {
        try {
            MemoryAPI mem = Global.getSector().getCharacterData().getMemory();
            Object v = mem.get(USES_KEY);
            int current = v instanceof Number ? ((Number) v).intValue() : 0;
            mem.set(USES_KEY, (float) Math.max(0, current - 1), 0f);
        } catch (Exception ignored) {}
    }
}
