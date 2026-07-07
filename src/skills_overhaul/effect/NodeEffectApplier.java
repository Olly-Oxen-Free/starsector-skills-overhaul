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

    /** Public gating check for combat-time appliers (T0 lane, oath, requiresActiveBasic). */
    public static boolean isEffectActive(Node n, JSONObject fx) {
        MemoryAPI mem = mem();
        boolean isT0 = (n.getY() == 0);
        boolean t0Active = isT0 && ActiveLaneService.isActive(mem, n.getId());
        return shouldApply(fx, isT0, t0Active, mem);
    }

    private static boolean shouldApply(JSONObject fx, boolean isT0, boolean t0Active, MemoryAPI mem) {
        // Oath filter — skip effect if it requires an oath that isn't currently active
        String requiredOath = fx.optString("oath", null);
        if (requiredOath != null) {
            skills_overhaul.zealot.Oath current = skills_overhaul.zealot.OathService.getCurrentOath(
                    com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate());
            if (current == null || !current.name().equals(requiredOath)) return false;
        }
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
