package skills_overhaul.effect;

import org.json.JSONObject;
import skills_overhaul.tree.Node;

public final class NodeEffectApplier {
    public static void apply(Node n) {
        for (JSONObject fx : n.getEffects()) {
            EffectRegistry.get(fx.optString("type")).apply(n.getId(), fx);
        }
    }
    public static void remove(Node n) {
        for (JSONObject fx : n.getEffects()) {
            EffectRegistry.get(fx.optString("type")).remove(n.getId(), fx);
        }
    }
}
