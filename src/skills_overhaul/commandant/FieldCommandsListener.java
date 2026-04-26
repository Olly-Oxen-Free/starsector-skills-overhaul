package skills_overhaul.commandant;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.List;

public final class FieldCommandsListener extends BaseEveryFrameCombatPlugin {
    private final IntervalUtil interval = new IntervalUtil(0.25f, 0.25f);
    private boolean reset = false;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!reset) {
            FieldCommandsService.resetForBattle();
            reset = true;
        }
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        FieldCommandsService.tick(0.25f);
    }
}
