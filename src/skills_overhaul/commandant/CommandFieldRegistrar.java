package skills_overhaul.commandant;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandFieldRegistrar extends BaseEveryFrameCombatPlugin {

    private final Set<ShipAPI> attached = new HashSet<>();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null) return;
        if (!engine.isInCampaign() && !engine.isInCampaignSim()) return; // no sector memory in missions
        // Command Field is the commandant_t0 core channel (spec: "core: registers Command Field");
        // without this gate every player ship got the aura whether or not the node was owned/active.
        if (!Boolean.TRUE.equals(com.fs.starfarer.api.Global.getSector().getMemoryWithoutUpdate()
                .get(skills_overhaul.effect.handlers.SOCommandantT0Handler.K_CORE_ACTIVE))) return;
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            if (s.isFighter()) continue; // per-fighter O(ships) aura scans; fighters aren't aura ships
            if (attached.contains(s)) continue;
            CommandFieldService.attach(s);
            attached.add(s);
        }
    }
}
