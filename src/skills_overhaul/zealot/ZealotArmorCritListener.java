package skills_overhaul.zealot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

/**
 * Steelbound armor crit (T3): 10% base chance per hit on armor to deal +50% damage.
 * Per-Zeal-stack: chance scales up to +7.5% at cap 100.
 *
 * Signature from DamageDealtModifier (Shape A):
 *   String modifyDamageDealt(Object, CombatEntityAPI, DamageAPI, Vector2f, boolean)
 */
public final class ZealotArmorCritListener implements DamageDealtModifier {

    private static final String KEY = "zealot_t3_steelbound_crit";
    private static final float BASE_CHANCE = 0.10f;
    private static final float MAX_STACK_BONUS = 0.075f;
    private static final float STACK_RATE = 0.00075f;
    private static final float CRIT_MULT = 1.50f;

    private final ShipAPI ship;
    private final Random rng = new Random();

    public ZealotArmorCritListener(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target,
                                    DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit) return null;
        if (damage == null) return null;

        int zealStacks = ZealService.getStacks(Global.getSector().getMemoryWithoutUpdate());
        float chance = BASE_CHANCE + Math.min(MAX_STACK_BONUS, STACK_RATE * zealStacks);
        if (rng.nextFloat() > chance) return null;

        damage.getModifier().modifyMult(KEY, CRIT_MULT);
        return KEY;
    }
}
