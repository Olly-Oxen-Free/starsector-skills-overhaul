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
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0) continue;
            if (attached.contains(s)) continue;
            CommandFieldService.attach(s);
            attached.add(s);
        }
    }
}
