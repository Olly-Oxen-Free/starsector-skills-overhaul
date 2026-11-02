package skills_overhaul.zealot;

import java.util.HashMap;
import java.util.Map;

public class CrusadeServiceTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();
        long now = 1_000_000L;
        assert !CrusadeService.isActive(mem);
        assert CrusadeService.canDeclare(mem, now);

        CrusadeService.declare(mem, "pirates", now);
        assert CrusadeService.isActive(mem);
        assert "pirates".equals(CrusadeService.getCrusadeRival(mem));

        long sixMonthsLater = now + 180L * 86400L * 1000L;
        assert !CrusadeService.canManualCancel(mem, now + 30L * 86400L * 1000L);
        assert CrusadeService.canManualCancel(mem, sixMonthsLater);

        CrusadeService.manualCancel(mem, sixMonthsLater);
        assert !CrusadeService.isActive(mem);

        assert !CrusadeService.canDeclare(mem, sixMonthsLater);
        long oneYearLater = sixMonthsLater + 180L * 86400L * 1000L;
        assert CrusadeService.canDeclare(mem, oneYearLater);

        // rivalDestroyed ends the crusade naturally: no cooldown, rival cleared.
        Map<String,Object> mem2 = new HashMap<>();
        CrusadeService.declare(mem2, "hegemony", now);
        assert CrusadeService.isActive(mem2);
        CrusadeService.rivalDestroyed(mem2);
        assert !CrusadeService.isActive(mem2);
        assert CrusadeService.getCrusadeRival(mem2) == null;
        assert CrusadeService.canDeclare(mem2, now) : "no cooldown after natural end";
        assert CrusadeService.CAP_BONUS == 50 : "crusade Zeal-cap bonus is +50";

        // Removing the T5 capstone mid-Crusade ends it and drops the +50 cap bonus.
        Map<String,Object> mem3 = new HashMap<>();
        int baseCap = ZealService.getCap(mem3);
        CrusadeService.declare(mem3, "hegemony", now);
        ZealService.registerCapBonus(mem3, "crusade", CrusadeService.CAP_BONUS);
        assert ZealService.getCap(mem3) == baseCap + CrusadeService.CAP_BONUS;
        CrusadeService.endForCapstoneRemoval(mem3);
        assert !CrusadeService.isActive(mem3) : "crusade ends when T5 is removed";
        assert CrusadeService.getCrusadeRival(mem3) == null;
        assert ZealService.getCap(mem3) == baseCap : "crusade cap bonus removed with T5";
        // No-op when no Crusade is active.
        Map<String,Object> mem4 = new HashMap<>();
        CrusadeService.endForCapstoneRemoval(mem4);
        assert !CrusadeService.isActive(mem4);
        assert ZealService.getCap(mem4) == baseCap;

        System.out.println("ALL PASSED");
    }
}
