package skills_overhaul.supply;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.Map;

/**
 * Scrap buffer: a virtual ability-resource pool (SiC "scrap" replacement),
 * fed by a fraction of post-battle salvage — never skimmed from fleet cargo.
 * Cap scales with commander level and fleet cargo capacity.
 * Drain modes:
 *   ABILITY — buffer first, then fleet supplies when the buffer runs dry.
 *   UPKEEP  — fleet only; buffer untouched.
 *
 * Gated on commander progression (isUnlocked): no commander levels, no pool.
 * Service is pure logic over the memory Map. Listener / renderer integrate it with the game.
 */
public final class SupplyBufferService {

    public static final float INCOME_BONUS_MULTIPLIER = 1.10f;
    /** Fraction of salvaged supplies mirrored into the buffer as scrap (loot itself untouched). */
    public static final float SALVAGE_TO_BUFFER_RATIO = 0.25f;
    public static final float BASE_CAP = 20f;
    public static final float CAP_PER_LEVEL = 5f;
    public static final float FLEET_CARGO_RATIO = 0.10f;

    private static final java.util.Map<String, Float> CAP_MULTIPLIERS = new java.util.LinkedHashMap<>();

    public enum DrainMode { ABILITY, UPKEEP }

    public static final class DrainResult {
        public float fromBuffer;
        public float fromFleet;
        /** Amount the caller still owes if fleet didn't have enough; the caller decides what to do. */
        public float shortfall;
    }

    /** The pool exists only once the commander tree has progressed. */
    public static boolean isUnlocked(Map<String,Object> mem) {
        return SkillMemory.getLevel(mem, Tree.COMMANDER) > 0;
    }

    public static boolean isUnlocked(MemoryAPI mem) {
        return SkillMemory.getLevel(mem, Tree.COMMANDER) > 0;
    }

    /** cap = BASE_CAP + CAP_PER_LEVEL * commanderLevel + floor(FLEET_CARGO_RATIO * fleetCargoCapacity), multiplied by cap multipliers. */
    public static float computeCap(Map<String,Object> mem, float fleetCargoCapacity) {
        int lvl = SkillMemory.getLevel(mem, Tree.COMMANDER);
        float fleetPart = (float) Math.floor(FLEET_CARGO_RATIO * fleetCargoCapacity);
        return (BASE_CAP + CAP_PER_LEVEL * lvl + fleetPart) * capMultiplierProduct();
    }

    /**
     * Adds supplies into the buffer up to the cap. Returns the overflow amount that did
     * not fit (the caller routes overflow into normal fleet cargo).
     */
    public static float add(Map<String,Object> mem, float fleetCargoCapacity, float amount) {
        if (amount <= 0f) return 0f;
        float cap = computeCap(mem, fleetCargoCapacity);
        float cur = SkillMemory.getSupplyBuffer(mem);
        float room = Math.max(0f, cap - cur);
        float take = Math.min(room, amount);
        SkillMemory.setSupplyBuffer(mem, cur + take);
        return amount - take;
    }

    /**
     * Attempts to consume `amount`. Buffer is drained first when mode is ABILITY; never when UPKEEP.
     * Returns a breakdown. The caller is responsible for actually deducting `fromFleet` from the
     * fleet's cargo and detecting an over-draft (insufficient fleet cargo); see SupplyBufferListener.
     */
    public static DrainResult tryConsume(Map<String,Object> mem, float amount, DrainMode mode) {
        DrainResult r = new DrainResult();
        if (amount <= 0f) return r;
        float remaining = amount;
        if (mode == DrainMode.ABILITY) {
            float cur = SkillMemory.getSupplyBuffer(mem);
            float take = Math.min(cur, remaining);
            r.fromBuffer = take;
            SkillMemory.setSupplyBuffer(mem, cur - take);
            remaining -= take;
        }
        r.fromFleet = remaining;
        return r;
    }

    public static float applyIncomeBonus(float baseAmount) {
        return baseAmount * INCOME_BONUS_MULTIPLIER;
    }

    public static void registerCapMultiplier(String key, float mult) {
        CAP_MULTIPLIERS.put(key, mult);
    }

    public static void unregisterCapMultiplier(String key) {
        CAP_MULTIPLIERS.remove(key);
    }

    /** Load-time reset — statics survive across save loads in the same JVM. */
    public static void clearCapMultipliers() {
        CAP_MULTIPLIERS.clear();
    }

    private static float capMultiplierProduct() {
        float p = 1f;
        for (Float m : CAP_MULTIPLIERS.values()) p *= m;
        return p;
    }

    // MemoryAPI overloads — mirrors the SkillMemory pattern for live-game use.
    public static float computeCap(MemoryAPI mem, float fleetCargoCapacity) {
        int lvl = SkillMemory.getLevel(mem, Tree.COMMANDER);
        float fleetPart = (float) Math.floor(FLEET_CARGO_RATIO * fleetCargoCapacity);
        return (BASE_CAP + CAP_PER_LEVEL * lvl + fleetPart) * capMultiplierProduct();
    }

    public static float add(MemoryAPI mem, float fleetCargoCapacity, float amount) {
        if (amount <= 0f) return 0f;
        float cap = computeCap(mem, fleetCargoCapacity);
        float cur = SkillMemory.getSupplyBuffer(mem);
        float room = Math.max(0f, cap - cur);
        float take = Math.min(room, amount);
        SkillMemory.setSupplyBuffer(mem, cur + take);
        return amount - take;
    }

    public static DrainResult tryConsume(MemoryAPI mem, float amount, DrainMode mode) {
        DrainResult r = new DrainResult();
        if (amount <= 0f) return r;
        float remaining = amount;
        if (mode == DrainMode.ABILITY) {
            float cur = SkillMemory.getSupplyBuffer(mem);
            float take = Math.min(cur, remaining);
            r.fromBuffer = take;
            SkillMemory.setSupplyBuffer(mem, cur - take);
            remaining -= take;
        }
        r.fromFleet = remaining;
        return r;
    }

    private SupplyBufferService() {}
}
