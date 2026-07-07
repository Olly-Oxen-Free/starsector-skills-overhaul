package skills_overhaul.artificer.alphastorm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.artificer.alphastorm.AlphaStormState.Intensity;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;

public final class AlphaStormBattleScript extends BaseEveryFrameCombatPlugin {

    public static final String STAT_KEY = "skills_overhaul_alpha_storm_terrain";

    private StormShieldObjective shield;
    private boolean firstFrame = true;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        // Only run if Alpha Storm campaign ability is active
        if (!AlphaStormState.isActive(Global.getSector().getMemoryWithoutUpdate())) return;

        int today = AlphaStormAbility.monotonicDay();
        Intensity intensity = AlphaStormState.intensity(
                Global.getSector().getMemoryWithoutUpdate(), today);
        if (intensity == null) return;

        if (firstFrame) {
            firstFrame = false;
            // The shelter exists only around a player-deployed Alpha Storm Emitter. If no
            // emitter was deployed this battle (flag set by ForgeBattlePlugin), there is no
            // shelter and every ship is subject to storm penalties.
            boolean emitterDeployed = Boolean.TRUE.equals(
                    Global.getSector().getMemoryWithoutUpdate().get(
                            skills_overhaul.artificer.forge.ForgeBattlePlugin.SHELTER_ACTIVE_KEY));
            if (emitterDeployed) {
                Vector2f anchor = new Vector2f(0f, 0f);
                ShipAPI flagship = engine.getPlayerShip();
                if (flagship != null && flagship.getLocation() != null) {
                    anchor = new Vector2f(flagship.getLocation());
                }
                shield = new StormShieldObjective(anchor);
            }
        }
        if (shield != null) shield.tick(engine);

        // Apply per-ship terrain mods
        for (ShipAPI s : engine.getShips()) {
            if (!s.isAlive()) continue;
            if (shield != null && shield.isSheltered(s)) {
                s.getMutableStats().getMaxSpeed().unmodify(STAT_KEY);
                s.getMutableStats().getBallisticWeaponRangeBonus().unmodify(STAT_KEY);
                s.getMutableStats().getEnergyWeaponRangeBonus().unmodify(STAT_KEY);
                s.getMutableStats().getCRPerDeploymentPercent().unmodify(STAT_KEY);
                continue;
            }
            float maneuverMult, rangeMult, crLossMult;
            switch (intensity) {
                case MILD:     maneuverMult = 0.90f; rangeMult = 0.95f; crLossMult = 2f; break;
                case MODERATE: maneuverMult = 0.80f; rangeMult = 0.90f; crLossMult = 2.5f; break;
                case SEVERE:   maneuverMult = 0.70f; rangeMult = 0.85f; crLossMult = 3f; break;
                default:       continue;
            }
            s.getMutableStats().getMaxSpeed().modifyMult(STAT_KEY, maneuverMult);
            s.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(STAT_KEY, rangeMult);
            s.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(STAT_KEY, rangeMult);
            try {
                s.getMutableStats().getCRPerDeploymentPercent().modifyMult(STAT_KEY, crLossMult);
            } catch (Throwable t) {}

            // Lightning strikes
            AlphaStormLightning.rollAndStrike(engine, s, intensity, amount);
        }
    }
}
