package skills_overhaul.supply;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.Map;

public class SupplyBufferServiceTest {
    public static void main(String[] args) throws Exception {
        // --- cap formula: 20 + 5*level + floor(0.10 * fleetCargo) ---
        Map<String,Object> mem = new HashMap<>();
        SkillMemory.setLevel(mem, Tree.COMMANDER, 0);
        assert SupplyBufferService.computeCap(mem, 0f) == 20f;
        assert SupplyBufferService.computeCap(mem, 100f) == 30f : "cap @ L0/c100 -> 20 + 10";
        SkillMemory.setLevel(mem, Tree.COMMANDER, 5);
        assert SupplyBufferService.computeCap(mem, 0f) == 45f : "cap @ L5/c0 -> 20 + 25";
        SkillMemory.setLevel(mem, Tree.COMMANDER, 10);
        assert SupplyBufferService.computeCap(mem, 250f) == 95f : "cap @ L10/c250 -> 70 + 25";

        // --- add: fills until cap, returns overflow ---
        Map<String,Object> mem2 = new HashMap<>();
        SkillMemory.setLevel(mem2, Tree.COMMANDER, 0);
        // cap = 20
        float over = SupplyBufferService.add(mem2, 0f, 15f);
        assert over == 0f && SkillMemory.getSupplyBuffer(mem2) == 15f;
        over = SupplyBufferService.add(mem2, 0f, 10f);
        assert over == 5f : "overflow = 5";
        assert SkillMemory.getSupplyBuffer(mem2) == 20f : "buffer at cap";

        // --- tryConsume ABILITY: buffer-first then fleet ---
        Map<String,Object> mem3 = new HashMap<>();
        SkillMemory.setLevel(mem3, Tree.COMMANDER, 0);
        SkillMemory.setSupplyBuffer(mem3, 12f);
        SupplyBufferService.DrainResult r =
                SupplyBufferService.tryConsume(mem3, 8f, SupplyBufferService.DrainMode.ABILITY);
        assert r.fromBuffer == 8f && r.fromFleet == 0f && r.shortfall == 0f;
        assert SkillMemory.getSupplyBuffer(mem3) == 4f;

        r = SupplyBufferService.tryConsume(mem3, 10f, SupplyBufferService.DrainMode.ABILITY);
        assert r.fromBuffer == 4f : "drained remaining 4 from buffer";
        assert r.fromFleet == 6f : "remaining 6 charged to fleet";
        assert r.shortfall == 0f : "shortfall is caller's job to detect (fromFleet > availableFleet)";
        assert SkillMemory.getSupplyBuffer(mem3) == 0f;

        // --- tryConsume UPKEEP: fleet-first; buffer untouched ---
        Map<String,Object> mem4 = new HashMap<>();
        SkillMemory.setLevel(mem4, Tree.COMMANDER, 0);
        SkillMemory.setSupplyBuffer(mem4, 15f);
        r = SupplyBufferService.tryConsume(mem4, 10f, SupplyBufferService.DrainMode.UPKEEP);
        assert r.fromBuffer == 0f : "UPKEEP never touches buffer";
        assert r.fromFleet == 10f;
        assert SkillMemory.getSupplyBuffer(mem4) == 15f : "buffer unchanged";

        // --- applyIncomeBonus: +10% multiplier ---
        assert SupplyBufferService.applyIncomeBonus(100f) == 110f;
        assert SupplyBufferService.applyIncomeBonus(0f) == 0f;
        assert Math.abs(SupplyBufferService.applyIncomeBonus(33f) - 33f * 1.10f) < 1e-4f;

        System.out.println("ALL PASSED");
    }
}
