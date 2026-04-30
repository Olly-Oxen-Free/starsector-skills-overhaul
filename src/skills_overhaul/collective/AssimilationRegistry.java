package skills_overhaul.collective;

/**
 * Tracks how many AI cores of each tier have been assimilated, and computes
 * the resulting fleet-wide passive bonuses (capped).
 */
public final class AssimilationRegistry {

    // [TUNE] per-core bonus rates and caps
    private static final float GAMMA_CR_PER_CORE     = 1f;
    private static final float BETA_FLUX_PER_CORE    = 2f;
    private static final float ALPHA_DAMAGE_PER_CORE = 4f;
    private static final float GAMMA_CR_CAP          = 10f;
    private static final float BETA_FLUX_CAP         = 10f;
    private static final float ALPHA_DAMAGE_CAP      = 12f;

    private static final String GAMMA_KEY = "$so_collective.assimilation.gamma";
    private static final String BETA_KEY  = "$so_collective.assimilation.beta";
    private static final String ALPHA_KEY = "$so_collective.assimilation.alpha";

    private AssimilationRegistry() {}

    private static int get(MemoryView mem, String key) {
        Object v = mem.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    public static int getGammaCount(MemoryView mem) { return get(mem, GAMMA_KEY); }
    public static int getBetaCount(MemoryView mem)  { return get(mem, BETA_KEY);  }
    public static int getAlphaCount(MemoryView mem) { return get(mem, ALPHA_KEY); }

    public static void assimilateGamma(MemoryView mem) { mem.set(GAMMA_KEY, getGammaCount(mem) + 1); }
    public static void assimilateBeta(MemoryView mem)  { mem.set(BETA_KEY,  getBetaCount(mem) + 1);  }
    public static void assimilateAlpha(MemoryView mem) { mem.set(ALPHA_KEY, getAlphaCount(mem) + 1); }

    /** +1% max CR per gamma core, capped at 10%. */
    public static float getCRBonus(MemoryView mem) {
        return Math.min(GAMMA_CR_CAP, getGammaCount(mem) * GAMMA_CR_PER_CORE);
    }

    /** +2% flux dissipation per beta core, capped at 10%. */
    public static float getFluxDissipationBonus(MemoryView mem) {
        return Math.min(BETA_FLUX_CAP, getBetaCount(mem) * BETA_FLUX_PER_CORE);
    }

    /** +4% weapon damage per alpha core, capped at 12%. */
    public static float getWeaponDamageBonus(MemoryView mem) {
        return Math.min(ALPHA_DAMAGE_CAP, getAlphaCount(mem) * ALPHA_DAMAGE_PER_CORE);
    }
}
