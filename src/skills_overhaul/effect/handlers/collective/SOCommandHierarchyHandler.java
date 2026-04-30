package skills_overhaul.effect.handlers.collective;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import skills_overhaul.collective.CollectiveActiveNodes;
import skills_overhaul.effect.EffectHandler;

public final class SOCommandHierarchyHandler implements EffectHandler {

    private static final String MOD_KEY = "so_command_hierarchy";

    @Override
    public void apply(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.add(nodeId);
        // Campaign: +40 automated ship points
        try {
            if (Global.getSector() != null && Global.getSector().getPlayerPerson() != null) {
                Global.getSector().getPlayerPerson()
                        .getStats().getDynamic().getMod("so_automated_ship_points")
                        .modifyFlat(MOD_KEY, 40f);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        CollectiveActiveNodes.ACTIVE.remove(nodeId);
        try {
            if (Global.getSector() != null && Global.getSector().getPlayerPerson() != null) {
                Global.getSector().getPlayerPerson()
                        .getStats().getDynamic().getMod("so_automated_ship_points")
                        .unmodify(MOD_KEY);
            }
        } catch (Exception ignored) {}
    }
}
