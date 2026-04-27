package skills_overhaul.zealot;

import java.util.HashMap;
import java.util.Map;

public class RivalryServiceTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();
        assert RivalryService.getCurrentRival(mem) == null;

        long now = 1_000_000L;
        assert RivalryService.canDesignate(mem, now);
        RivalryService.designate(mem, "pirates", now);
        assert "pirates".equals(RivalryService.getCurrentRival(mem));

        long oneDayLater = now + 86400L * 1000L;
        assert !RivalryService.canDesignate(mem, oneDayLater);
        long thirtyDaysLater = now + 30L * 86400L * 1000L;
        assert RivalryService.canDesignate(mem, thirtyDaysLater);

        RivalryService.designate(mem, "hegemony", thirtyDaysLater);
        assert "hegemony".equals(RivalryService.getCurrentRival(mem));

        assert RivalryService.isRival(mem, "hegemony");
        assert !RivalryService.isRival(mem, "pirates");

        System.out.println("ALL PASSED");
    }
}
