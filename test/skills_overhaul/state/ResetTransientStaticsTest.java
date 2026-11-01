package skills_overhaul.state;

import skills_overhaul.commandant.FieldCommandsService;
import skills_overhaul.effect.handlers.corsair.BountyBoardListener;
import skills_overhaul.effect.handlers.corsair.PostBattleSalvageMultiplierListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Regression for review 2026-11-01-2306-effect B3: statics written by effect handlers
 * must be cleared on load, or a save loaded after another one in the same session
 * inherits its bounty payouts, loot multipliers and Field Commands.
 */
public class ResetTransientStaticsTest {
    public static void main(String[] args) throws Exception {
        // State left behind by save A
        BountyBoardListener.active = true;
        BountyBoardListener.weaponDropMult = 1.5f;
        FieldCommandsService.active = true;
        PostBattleSalvageMultiplierListener.register("corsair_salvage_a", 1.2f);
        PostBattleSalvageMultiplierListener.register("corsair_salvage_b", 1.3f);

        Method reset = NodeApplyService.class.getDeclaredMethod("resetTransientStatics");
        reset.setAccessible(true);
        reset.invoke(null);

        assert !BountyBoardListener.active : "BountyBoardListener.active survives load";
        assert BountyBoardListener.weaponDropMult == 1f : "BountyBoardListener.weaponDropMult survives load";
        assert !FieldCommandsService.active : "FieldCommandsService.active survives load";

        Field f = PostBattleSalvageMultiplierListener.class.getDeclaredField("MULTIPLIERS");
        f.setAccessible(true);
        assert ((Map<?, ?>) f.get(null)).isEmpty() : "salvage multipliers survive load";

        System.out.println("ALL PASSED");
    }
}
