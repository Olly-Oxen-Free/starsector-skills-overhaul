package skills_overhaul.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.json.JSONObject;
import skills_overhaul.basics.ActiveLaneService;
import skills_overhaul.tree.Node;

public final class NodeEffectApplier {

    public static void apply(Node n) {
        MemoryAPI mem = mem();
        boolean isT0 = (n.getY() == 0);
        boolean t0Active = isT0 && ActiveLaneService.isActive(mem, n.getId());
        for (JSONObject fx : n.getEffects()) {
            if (!shouldApply(fx, isT0, t0Active, mem)) continue;
            EffectRegistry.get(fx.optString("type")).apply(n.getId(), fx);
        }
    }

    public static void remove(Node n) {
        MemoryAPI mem = mem();
        boolean isT0 = (n.getY() == 0);
        boolean t0Active = isT0 && ActiveLaneService.isActive(mem, n.getId());
        for (JSONObject fx : n.getEffects()) {
            if (!shouldApply(fx, isT0, t0Active, mem)) continue;
            EffectRegistry.get(fx.optString("type")).remove(n.getId(), fx);
        }
    }

    /**
     * Re-evaluate gating for a single node — call after an active-set change so the
     * core/base effect set in the world matches the new active set. Non-T0 nodes are unaffected.
     */
    public static void reapplyForActiveChange(Node n, boolean nowActive) {
        if (n.getY() != 0) return;
        for (JSONObject fx : n.getEffects()) {
            String channel = fx.optString("channel", "base");
            if (!"core".equals(channel)) continue;
            EffectHandler h = EffectRegistry.get(fx.optString("type"));
            if (nowActive) h.apply(n.getId(), fx);
            else h.remove(n.getId(), fx);
        }
    }

    private static boolean shouldApply(JSONObject fx, boolean isT0, boolean t0Active, MemoryAPI mem) {
        if (isT0) {
            String channel = fx.optString("channel", "base");
            if ("core".equals(channel)) return t0Active;
            return true;
        }
        if (fx.optBoolean("requiresActiveBasic", false)) {
            return !ActiveLaneService.getActive(mem).isEmpty();
        }
        return true;
    }

    private static MemoryAPI mem() {
        return Global.getSector().getMemoryWithoutUpdate();
    }

    private NodeEffectApplier() {}
}
