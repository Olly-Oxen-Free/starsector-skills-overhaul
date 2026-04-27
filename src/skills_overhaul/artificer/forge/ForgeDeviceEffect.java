package skills_overhaul.artificer.forge;

import com.fs.starfarer.api.combat.CombatEngineAPI;

public interface ForgeDeviceEffect {
    default void deployBattle(CombatEngineAPI engine, boolean overQuota) {}
    default void deployCampaign() {}
}
