package skills_overhaul.artificer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.support.InvincibilityListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FinalGambitService {

    public static final float TRIGGER_HULL_PCT = 0.20f;
    public static final float DURATION_SECONDS = 4f;
    public static final float EXPLOSION_RADIUS = 1500f;
    public static final float EXPLOSION_BASE_DAMAGE = 5000f;
    public static final float EXPLOSION_PER_DP = 100f;
    public static final float SPEED_BOOST_MULT = 2.5f;
    public static final String STAT_KEY = "skills_overhaul_final_gambit";

    private static final Set<ShipAPI> triggered = new HashSet<>();

    public static void resetForBattle() { triggered.clear(); }

    public static void tick(CombatEngineAPI engine) {
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isAlive()) continue;
            if (triggered.contains(s)) continue;
            if (s.isFighter() || !isAutomated(s)) continue;
            float hullPct = s.getHitpoints() / Math.max(1f, s.getMaxHitpoints());
            if (hullPct > TRIGGER_HULL_PCT) continue;
            triggerOn(s);
        }
    }

    private static void triggerOn(final ShipAPI s) {
        triggered.add(s);
        // Invuln + speed boost
        final InvincibilityListener listener = new InvincibilityListener(s, DURATION_SECONDS);
        s.addListener(listener);
        s.getMutableStats().getMaxSpeed().modifyMult(STAT_KEY, SPEED_BOOST_MULT);

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            float remaining = DURATION_SECONDS;
            @Override public void advance(float amount, List<InputEventAPI> events) {
                listener.advance(amount);
                remaining -= amount;
                if (remaining <= 0f) {
                    explode(s);
                    s.getMutableStats().getMaxSpeed().unmodify(STAT_KEY);
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    private static void explode(ShipAPI s) {
        if (s == null || !s.isAlive()) return;
        float dp = 0f;
        try { dp = s.getFleetMember() != null ? s.getFleetMember().getDeploymentPointsCost() : 20f; } catch (Throwable t) { dp = 20f; }
        float damage = EXPLOSION_BASE_DAMAGE + dp * EXPLOSION_PER_DP;

        // Apply damage to enemies in radius
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other == s || other.getOwner() != 1 || !other.isAlive()) continue;
            float dist = com.fs.starfarer.api.util.Misc.getDistance(s.getLocation(), other.getLocation());
            if (dist > EXPLOSION_RADIUS) continue;
            float falloff = 1f - (dist / EXPLOSION_RADIUS);
            Global.getCombatEngine().applyDamage(
                    other, other.getLocation(), damage * falloff,
                    DamageType.HIGH_EXPLOSIVE, 0f, false, false, s);
        }

        // Destroy own ship. setHitpoints(0) alone does not run the engine's death path;
        // the invincibility listener has expired by now (same duration, advanced first).
        Global.getCombatEngine().applyDamage(s, s.getLocation(),
                999_999f, DamageType.ENERGY, 0f, true, false, s);
    }

    private static boolean isAutomated(ShipAPI s) {
        // Vanilla automated hulls carry the built-in "automated" hullmod, not a hullspec tag.
        return com.fs.starfarer.api.util.Misc.isAutomated(s);
    }

    private FinalGambitService() {}
}
