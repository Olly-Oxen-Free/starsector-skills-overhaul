package skills_overhaul.commandant;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import java.util.List;

public final class ObjectiveTracker {

    public static int countOwnedBy(List<Integer> owners, int side) {
        int n = 0;
        for (Integer o : owners) if (o != null && o == side) n++;
        return n;
    }

    public static int total(List<Integer> owners) {
        return owners == null ? 0 : owners.size();
    }

    public static int countLive(int side) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return 0;
        int n = 0;
        for (BattleObjectiveAPI o : engine.getObjectives()) {
            if (o.getOwner() == side) n++;
        }
        return n;
    }

    public static int totalLive() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return 0;
        return engine.getObjectives().size();
    }

    private ObjectiveTracker() {}
}
