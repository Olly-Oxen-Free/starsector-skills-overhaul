package skills_overhaul.xp;

import java.util.HashMap;
import java.util.Map;
import skills_overhaul.api.SkillsOverhaulAPI;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

public class CommanderXPMirrorTest {
    public static void main(String[] args) {
        // Default provider → neutral 1.0 multiplier.
        SkillsOverhaulAPI.resetProvider();
        assert Math.abs(CommanderXPListener.commanderMultiplier() - 1.0f) < 1e-6 : "default mult = 1.0";

        // Neutral multiplier mirrors 1:1 (same behaviour reportPlayerEngagement uses).
        Map<String,Object> mem = new HashMap<>();
        long d1 = 100L, d2 = 50L;
        SkillMemory.addXP(mem, Tree.COMMANDER, (float) d1 * CommanderXPListener.commanderMultiplier());
        SkillMemory.addXP(mem, Tree.COMMANDER, (float) d2 * CommanderXPListener.commanderMultiplier());
        assert Math.abs(SkillMemory.getXP(mem, Tree.COMMANDER) - 150f) < 1e-3 : "neutral mirror 1:1";
        assert SkillMemory.getXP(mem, Tree.PILOT) == 0f : "pilot untouched";
        assert SkillMemory.getXP(mem, Tree.ADMIN) == 0f : "admin untouched";

        // Sub-mod provider (crew XP) layers a multiplier onto the mirrored delta.
        SkillsOverhaulAPI.setCommanderMultiplierProvider(new SkillsOverhaulAPI.CommanderMultiplierProvider() {
            @Override public float getCommanderNumericMultiplier() { return 1.20f; }
        });
        assert Math.abs(CommanderXPListener.commanderMultiplier() - 1.20f) < 1e-6 : "provider mult applied";
        Map<String,Object> mem2 = new HashMap<>();
        SkillMemory.addXP(mem2, Tree.COMMANDER, (float) 100L * CommanderXPListener.commanderMultiplier());
        assert Math.abs(SkillMemory.getXP(mem2, Tree.COMMANDER) - 120f) < 1e-3 : "100 * 1.2 = 120";

        // Degenerate provider (0 / negative / NaN) is clamped to 1.0.
        SkillsOverhaulAPI.setCommanderMultiplierProvider(new SkillsOverhaulAPI.CommanderMultiplierProvider() {
            @Override public float getCommanderNumericMultiplier() { return 0f; }
        });
        assert Math.abs(CommanderXPListener.commanderMultiplier() - 1.0f) < 1e-6 : "zero mult clamped to 1.0";

        // resetProvider restores the neutral default.
        SkillsOverhaulAPI.resetProvider();
        assert Math.abs(CommanderXPListener.commanderMultiplier() - 1.0f) < 1e-6 : "reset → 1.0";

        System.out.println("ALL PASSED");
    }
}
