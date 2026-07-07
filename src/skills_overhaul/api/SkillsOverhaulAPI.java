package skills_overhaul.api;

public final class SkillsOverhaulAPI {
    /** Sub-mod (crew XP) overrides this to layer the integration multiplier (0.60..1.20).
     *  Default returns 1.00 so the core mod behaves identically without the sub-mod. */
    public interface CommanderMultiplierProvider {
        float getCommanderNumericMultiplier();
    }

    private static final CommanderMultiplierProvider DEFAULT_PROVIDER =
            new CommanderMultiplierProvider() {
                @Override public float getCommanderNumericMultiplier() { return 1.00f; }
            };

    private static CommanderMultiplierProvider provider = DEFAULT_PROVIDER;

    public static void setCommanderMultiplierProvider(CommanderMultiplierProvider p) {
        if (p != null) provider = p;
    }

    /**
     * Restore the neutral (1.00) default provider.
     *
     * Reset-safety note: this static field is mod-scoped, not save-scoped. The
     * crew-XP sub-mod registers its provider once at mod init (fresh JVM), so the
     * value is stable for the whole session regardless of save/reload — there is
     * no load hook among the wiring files that needs to clear it. This method
     * exists so a future load path can force the neutral default if the sub-mod
     * is ever unloaded mid-session.
     */
    public static void resetProvider() {
        provider = DEFAULT_PROVIDER;
    }

    public static float getCommanderNumericMultiplier() {
        return provider.getCommanderNumericMultiplier();
    }
}
