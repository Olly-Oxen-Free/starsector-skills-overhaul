package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public final class SynapticRelayService {

    private static final String HULLMOD_ID = "so_neural_uplink";

    public static void applyToFleet() {
        try {
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet()
                    .getFleetData().getMembersListCopy()) {
                if (member.isFighterWing()) continue;
                member.getVariant().addPermaMod(HULLMOD_ID);
            }
        } catch (Throwable t) {
            Global.getLogger(SynapticRelayService.class).warn(
                    "[SynapticRelay] applyToFleet failed: " + t.getMessage());
        }
    }

    public static void remove() {
        try {
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet()
                    .getFleetData().getMembersListCopy()) {
                if (member.isFighterWing()) continue;
                member.getVariant().removePermaMod(HULLMOD_ID);
            }
        } catch (Throwable t) {
            Global.getLogger(SynapticRelayService.class).warn(
                    "[SynapticRelay] remove failed: " + t.getMessage());
        }
    }

    public static void onCommandTransfer(ShipAPI newShip, CombatEngineAPI engine) {
        if (engine == null || newShip == null) return;
        engine.getTimeMult().modifyMult("so_synaptic_transfer", 1.25f);
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine() == null) return;
                elapsed += amount;
                if (elapsed >= 1f) {
                    Global.getCombatEngine().getTimeMult().unmodify("so_synaptic_transfer");
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    private SynapticRelayService() {}
}
