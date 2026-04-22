package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.xp.PilotXPTracker;

import java.util.List;

public class SkillsOverhaulPilotTrackerPlugin extends BaseEveryFrameCombatPlugin {
    private final PilotXPTracker delegate = new PilotXPTracker();

    @Override
    public void init(CombatEngineAPI engine) {
        delegate.init(engine);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        delegate.advance(amount, events);
    }
}
