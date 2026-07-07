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

        System.out.println("ALL PASSED");
    }
}
