package skills_overhaul.api;

public final class SkillsOverhaulAPI {
    /** Sub-mod (crew XP) overrides this to layer the integration multiplier (0.60..1.20).
     *  Default returns 1.00 so the core mod behaves identically without the sub-mod. */
    public interface CommanderMultiplierProvider {
        float getCommanderNumericMultiplier();
    }

    private static CommanderMultiplierProvider provider = new CommanderMultiplierProvider() {
        @Override public float getCommanderNumericMultiplier() { return 1.00f; }
    };

    public static void setCommanderMultiplierProvider(CommanderMultiplierProvider p) {
        if (p != null) provider = p;
    }

    public static float getCommanderNumericMultiplier() {
        return provider.getCommanderNumericMultiplier();
    }
}
