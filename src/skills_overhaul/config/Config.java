package skills_overhaul.config;

import org.json.JSONObject;

public final class Config {
    public final int   pointsPerLevel;
    public final int   bonusPointsEvery5;
    public final int   capstoneCost;
    public final int   mergeCost;
    public final int   nodeCost;
    public final float migrationMult;
    public final int   migrationCap;
    public final float autopilotXpMult;
    public final float flagshipFullSurvivalSeconds;
    public final float survivalBonusBaseRate;
    public final float fighterCreditMult;
    public final float underdogMin;
    public final float underdogMax;
    public final float killBonusThreshold;
    public final float killBonusAmount;
    public final int   goodStandingThreshold;
    public final int   stabilityStreakCapMonths;
    public final float vassalXpMult;
    public final float administratorXpMult;

    private Config(JSONObject j) {
        this.pointsPerLevel              = j.optInt("points_per_level", 1);
        this.bonusPointsEvery5           = j.optInt("bonus_points_every_5", 0);
        this.capstoneCost                = j.optInt("capstone_cost", 2);
        this.mergeCost                   = j.optInt("merge_cost", 1);
        this.nodeCost                    = j.optInt("node_cost", 1);
        this.migrationMult               = (float) j.optDouble("migration_mult", 1.5);
        this.migrationCap                = j.optInt("migration_cap", 15);
        this.autopilotXpMult             = (float) j.optDouble("autopilot_xp_mult", 0.80);
        this.flagshipFullSurvivalSeconds = (float) j.optDouble("flagship_full_survival_seconds", 60);
        this.survivalBonusBaseRate       = (float) j.optDouble("survival_bonus_base_rate", 0.20);
        this.fighterCreditMult           = (float) j.optDouble("fighter_credit_mult", 0.40);
        this.underdogMin                 = (float) j.optDouble("underdog_min", 0.75);
        this.underdogMax                 = (float) j.optDouble("underdog_max", 2.00);
        this.killBonusThreshold          = (float) j.optDouble("kill_bonus_threshold", 0.15);
        this.killBonusAmount             = (float) j.optDouble("kill_bonus_amount", 0.20);
        this.goodStandingThreshold       = j.optInt("good_standing_threshold", 5);
        this.stabilityStreakCapMonths    = j.optInt("stability_streak_cap_months", 6);
        this.vassalXpMult                = (float) j.optDouble("vassal_xp_mult", 0.25);
        this.administratorXpMult         = (float) j.optDouble("administrator_xp_mult", 0.50);
    }

    public static Config fromJSON(JSONObject j) { return new Config(j); }
}
