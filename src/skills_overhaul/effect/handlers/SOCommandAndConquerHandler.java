package skills_overhaul.effect.handlers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.json.JSONObject;
import skills_overhaul.effect.EffectHandler;

/**
 * Command and Conquer (commandant_t2_cnc), commandant tree spec §6:
 * CP recovery +50%; DP from objectives floors at 5% of battle size.
 * Player character stats — same levers as vanilla Command and Control
 * ({@link Stats#COMMAND_POINT_RATE_COMMANDER} +0.5) and SiC Command and Conquer
 * ({@link Stats#DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD} +0.05).
 */
public final class SOCommandAndConquerHandler implements EffectHandler {

    @Override
    public void apply(String nodeId, JSONObject params) {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerStats();
        if (s == null) return;
        s.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).modifyFlat(nodeId, 0.5f);
        s.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).modifyFlat(nodeId, 0.05f);
    }

    @Override
    public void remove(String nodeId, JSONObject params) {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerStats();
        if (s == null) return;
        s.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).unmodify(nodeId);
        s.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodify(nodeId);
    }
}
