package skills_overhaul.xp;

public final class PilotXPMath {
    public static final class Cfg {
        public float fighterCreditMult;
        public float underdogMin, underdogMax;
        public float killBonusThreshold, killBonusAmount;
        public float survivalBase;
        public float flagshipFullSurvivalSeconds;
        public float autopilotMult;
    }

    public static Cfg cfgDefault() {
        Cfg c = new Cfg();
        c.fighterCreditMult = 0.40f;
        c.underdogMin = 0.75f; c.underdogMax = 2.00f;
        c.killBonusThreshold = 0.15f; c.killBonusAmount = 0.20f;
        c.survivalBase = 0.20f;
        c.flagshipFullSurvivalSeconds = 60f;
        c.autopilotMult = 0.80f;
        return c;
    }

    public static Cfg cfgFromConfig(skills_overhaul.config.Config k) {
        Cfg c = new Cfg();
        c.fighterCreditMult = k.fighterCreditMult;
        c.underdogMin = k.underdogMin; c.underdogMax = k.underdogMax;
        c.killBonusThreshold = k.killBonusThreshold; c.killBonusAmount = k.killBonusAmount;
        c.survivalBase = k.survivalBonusBaseRate;
        c.flagshipFullSurvivalSeconds = k.flagshipFullSurvivalSeconds;
        c.autopilotMult = k.autopilotXpMult;
        return c;
    }

    public static float weightedDealt(float direct, float fighter, float fighterMult) {
        return Math.min(1f, direct + fighter * fighterMult);
    }

    public static float underdog(float enemyDP, float flagshipDP, float min, float max) {
        if (flagshipDP <= 0f) return min;
        return Math.max(min, Math.min(max, enemyDP / flagshipDP));
    }

    public static float killBonus(float weightedDealt, float threshold, float amount) {
        return weightedDealt >= threshold ? amount : 0f;
    }

    public static float shipReward(float shipXP, float directDealt, float fighterDealt,
                                   float enemyDP, float flagshipDP, Cfg c) {
        float weighted = weightedDealt(directDealt, fighterDealt, c.fighterCreditMult);
        float share    = weighted + killBonus(weighted, c.killBonusThreshold, c.killBonusAmount);
        float ud       = underdog(enemyDP, flagshipDP, c.underdogMin, c.underdogMax);
        return shipXP * share * ud;
    }

    public static float survivalBonus(boolean firstShipEverDied, boolean finalShipAlive,
                                      float sumShipXPKilled, float autoSec, float manualSec,
                                      float enemyDPEngaged, float flagshipDPEngaged, Cfg c) {
        if (firstShipEverDied) return 0f;
        if (!finalShipAlive)   return 0f;
        float difficulty = Math.max(0.5f, Math.min(3.0f,
                flagshipDPEngaged <= 0f ? 1f : enemyDPEngaged / flagshipDPEngaged));
        float effSec = c.autopilotMult * autoSec + 1.0f * manualSec;
        float timeFrac = Math.max(0f, Math.min(1f, effSec / c.flagshipFullSurvivalSeconds));
        return c.survivalBase * sumShipXPKilled * timeFrac * difficulty;
    }

    public static float effectiveByMode(float auto, float manual, float autopilotMult) {
        return autopilotMult * auto + 1.0f * manual;
    }
}
