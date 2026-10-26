package skills_overhaul.effect.handlers;

import skills_overhaul.zealot.ZealService;

import java.util.HashMap;
import java.util.Map;

public class RaidZealTest {
    public static void main(String[] args) {
        // Node owned: a raid grants one stack and stamps the gain day.
        Map<String, Object> owned = new HashMap<String, Object>();
        owned.put(SOHeavyDetachmentsHandler.K_PURCHASED, Boolean.TRUE);
        assert RaidZealListener.grantIfOwned(owned, 42);
        assert ZealService.getStacks(owned) == 1;
        assert ((Number) owned.get(ZealService.K_LAST_GAIN)).intValue() == 42;
        assert RaidZealListener.grantIfOwned(owned, 42);
        assert ZealService.getStacks(owned) == 2;

        // Node unowned (absent or false): nothing granted.
        Map<String, Object> none = new HashMap<String, Object>();
        assert !RaidZealListener.grantIfOwned(none, 42);
        assert ZealService.getStacks(none) == 0;
        none.put(SOHeavyDetachmentsHandler.K_PURCHASED, Boolean.FALSE);
        assert !RaidZealListener.grantIfOwned(none, 42);
        assert ZealService.getStacks(none) == 0;

        System.out.println("ALL PASSED");
    }
}
