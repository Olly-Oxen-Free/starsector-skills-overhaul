package skills_overhaul.api;

public class SkillsOverhaulAPITest {
    public static void main(String[] args) {
        assert Math.abs(SkillsOverhaulAPI.getCommanderNumericMultiplier() - 1.00f) < 1e-6 : "default";
        SkillsOverhaulAPI.setCommanderMultiplierProvider(
                new SkillsOverhaulAPI.CommanderMultiplierProvider() {
                    @Override public float getCommanderNumericMultiplier() { return 0.80f; }
                });
        assert Math.abs(SkillsOverhaulAPI.getCommanderNumericMultiplier() - 0.80f) < 1e-6 : "override";
        System.out.println("ALL PASSED");
    }
}
