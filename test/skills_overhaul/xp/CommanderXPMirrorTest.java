package skills_overhaul.xp;

import java.util.HashMap;
import java.util.Map;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

public class CommanderXPMirrorTest {
    public static void main(String[] args) {
        Map<String,Object> mem = new HashMap<>();

        CommanderXPListener.mirror(mem, 100f);
        CommanderXPListener.mirror(mem, 50f);
        assert Math.abs(SkillMemory.getXP(mem, Tree.COMMANDER) - 150f) < 1e-3 : "mirror 1:1";
        assert SkillMemory.getXP(mem, Tree.PILOT) == 0f : "pilot untouched";
        assert SkillMemory.getXP(mem, Tree.ADMIN) == 0f : "admin untouched";

        System.out.println("ALL PASSED");
    }
}
