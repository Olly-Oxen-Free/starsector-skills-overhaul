package skills_overhaul.xp;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.input.InputEventAPI;
import skills_overhaul.config.ConfigLoader;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PilotXPTracker extends BaseEveryFrameCombatPlugin {
    private CombatEngineAPI engine;
    private final DamageLedger ledger = new DamageLedger();
    private final Map<String, Float> enemyXPValue = new HashMap<String, Float>();
    private final Map<String, Float> enemyDP = new HashMap<String, Float>();
    private float flagshipDP = 10f;
    private float enemyDPEngaged = 0f;
    private String firstShipId = null;
    private boolean firstShipEverDied = false;
    private float autoSeconds = 0f;
    private float manualSeconds = 0f;
    private float totalKilledXP = 0f;
    private boolean bonusPaid = false;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (!engine.isInCampaign()) return;

        ShipAPI pf = engine.getPlayerShip();
        if (pf != null) {
            firstShipId = pf.getId();
            flagshipDP = dpOf(pf);
        }

        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 1) continue;
            float dp = dpOf(s);
            enemyDP.put(s.getId(), dp);
            enemyXPValue.put(s.getId(), dp * 10f);
            enemyDPEngaged += dp;
            s.addListener(new ShipDamageHook(this, s.getId()));
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused() || !engine.isInCampaign()) return;

        if (engine.isUIAutopilotOn()) autoSeconds += amount;
        else manualSeconds += amount;

        ShipAPI pf = engine.getPlayerShip();
        if (firstShipId != null && !firstShipEverDied) {
            if (pf == null || !pf.isAlive() || !firstShipId.equals(pf.getId())) {
                firstShipEverDied = true;
            }
        }

        if (!bonusPaid && isCombatEnding()) {
            payBattleEndBonus();
            bonusPaid = true;
        }
    }

    private boolean isCombatEnding() {
        // Simple v1 heuristic: all enemy ships hulked or gone
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() == 1 && s.isAlive()) return false;
        }
        return true;
    }

    private void payBattleEndBonus() {
        ShipAPI pf = engine.getPlayerShip();
        boolean finalShipAlive = pf != null && pf.isAlive();
        float bonus = PilotXPMath.survivalBonus(
                firstShipEverDied, finalShipAlive, totalKilledXP,
                autoSeconds, manualSeconds,
                enemyDPEngaged, flagshipDP,
                PilotXPMath.cfgFromConfig(ConfigLoader.get()));
        if (bonus > 0f) {
            SkillMemory.addXP(Global.getSector().getMemoryWithoutUpdate(), Tree.PILOT, bonus);
        }
    }

    public void recordDamage(String enemyShipId, float amount, boolean fromFighter, boolean autopilotOn) {
        if (fromFighter) ledger.addFighter(enemyShipId, amount, autopilotOn);
        else             ledger.addDirect(enemyShipId, amount, autopilotOn);
        ledger.addTotal(enemyShipId, amount);
    }

    public void onEnemyKilled(String enemyShipId) {
        Float xp = enemyXPValue.get(enemyShipId);
        if (xp == null) return;
        Float dp = enemyDP.get(enemyShipId);
        if (dp == null) dp = 10f;
        float mult = ConfigLoader.get().autopilotXpMult;
        float direct  = ledger.effectiveDirectShare(enemyShipId, mult);
        float fighter = ledger.effectiveFighterShare(enemyShipId, mult);
        float reward = PilotXPMath.shipReward(xp, direct, fighter, dp, flagshipDP,
                PilotXPMath.cfgFromConfig(ConfigLoader.get()));
        if (reward > 0f) {
            SkillMemory.addXP(Global.getSector().getMemoryWithoutUpdate(), Tree.PILOT, reward);
            totalKilledXP += xp;
        }
        enemyXPValue.remove(enemyShipId); // prevent double-pay
    }

    private float dpOf(ShipAPI s) {
        if (s.getFleetMember() != null) {
            float dp = s.getFleetMember().getDeploymentPointsCost();
            if (dp > 0f) return dp;
        }
        float sup = s.getHullSpec().getSuppliesToRecover();
        return sup > 0f ? sup : 5f;
    }
}

class ShipDamageHook implements DamageListener {
    private final PilotXPTracker tracker;
    private final String shipId;
    ShipDamageHook(PilotXPTracker t, String id) { this.tracker = t; this.shipId = id; }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI r) {
        if (!(target instanceof ShipAPI)) return;
        ShipAPI targetShip = (ShipAPI) target;
        if (targetShip.getOwner() != 1) return;

        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null) return;
        ShipAPI pf = engine.getPlayerShip();
        boolean autoOn = engine.isUIAutopilotOn();

        float total = r.getDamageToHull() + r.getTotalDamageToArmor() + r.getDamageToShields();
        if (total <= 0f) {
            // still check for death even on zero-damage flag events
            if (!targetShip.isAlive()) tracker.onEnemyKilled(shipId);
            return;
        }

        boolean fromPlayerShip = false;
        boolean fromPlayerFighter = false;
        if (source instanceof ShipAPI) {
            ShipAPI src = (ShipAPI) source;
            if (pf != null && src == pf) fromPlayerShip = true;
            else if (src.isFighter() && src.getWing() != null
                    && pf != null && src.getWing().getSourceShip() == pf) {
                fromPlayerFighter = true;
            }
        }
        if (fromPlayerShip || fromPlayerFighter) {
            tracker.recordDamage(shipId, total, fromPlayerFighter, autoOn);
        }

        if (!targetShip.isAlive()) tracker.onEnemyKilled(shipId);
    }
}
