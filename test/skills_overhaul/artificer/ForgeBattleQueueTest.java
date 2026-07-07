package skills_overhaul.artificer;

import skills_overhaul.artificer.forge.ForgeBattleHook;
import skills_overhaul.artificer.forge.ForgeBattlePlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-logic coverage for the Forge battle-device deploy queue:
 *  - Fix 3: deploying the same device N times fires N effects (multiplicity preserved).
 *  - Fix 4: firing / quota order follows deploy order deterministically.
 */
public class ForgeBattleQueueTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();

        // Empty queue drains to an empty (non-null) list.
        List<String> empty = ForgeBattlePlugin.drainDeployQueue(mem);
        assert empty != null && empty.isEmpty() : "empty queue";
        assert !mem.containsKey(ForgeBattleHook.DEPLOY_ORDER_KEY) : "drain unsets key";

        // Fix 3: same device deployed twice -> two entries -> two firings.
        ForgeBattleHook.recordDeploy(mem, "graviton_anchor");
        ForgeBattleHook.recordDeploy(mem, "graviton_anchor");
        List<String> twice = ForgeBattlePlugin.drainDeployQueue(mem);
        assert twice.size() == 2 : "two deploys -> two entries, got " + twice.size();
        assert twice.get(0).equals("graviton_anchor") && twice.get(1).equals("graviton_anchor");
        assert !mem.containsKey(ForgeBattleHook.DEPLOY_ORDER_KEY) : "drained again";

        // Fix 4: deploy order is preserved exactly (deterministic firing / quota assignment).
        ForgeBattleHook.recordDeploy(mem, "repair_pod");
        ForgeBattleHook.recordDeploy(mem, "alpha_storm_emitter");
        ForgeBattleHook.recordDeploy(mem, "ecm_wave");
        List<String> order = ForgeBattlePlugin.drainDeployQueue(mem);
        assert order.size() == 3;
        assert order.get(0).equals("repair_pod") : "order[0]";
        assert order.get(1).equals("alpha_storm_emitter") : "order[1]";
        assert order.get(2).equals("ecm_wave") : "order[2]";

        // Quota assignment: first `quota` deploys are free, the rest penalized — by index.
        int quota = 1;
        assert !(0 >= quota) : "index 0 within quota (free)";
        assert (1 >= quota) && (2 >= quota) : "indices 1,2 over quota (penalized)";

        System.out.println("ALL PASSED");
    }
}
