package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public final class CollectiveCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static final String SRC = "so_collective_assimilation";

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (!CollectiveActiveNodes.isActive("so_collective_t0")) return;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        applyAssimilationPassives(mem, engine);
    }

    public static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }

    private void applyAssimilationPassives(MemoryAPI mem, CombatEngineAPI engine) {
        MemoryView view = new MemoryAPIView(mem);
        float crBonus   = AssimilationRegistry.getCRBonus(view);
        float fluxBonus = AssimilationRegistry.getFluxDissipationBonus(view);
        float dmgBonus  = AssimilationRegistry.getWeaponDamageBonus(view);

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 0 || !ship.isAlive() || !isAICored(ship)) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            if (crBonus > 0f)
                stats.getMaxCombatReadiness().modifyPercent(SRC, crBonus);
            if (fluxBonus > 0f)
                stats.getFluxDissipation().modifyPercent(SRC, fluxBonus);
            if (dmgBonus > 0f) {
                stats.getBallisticWeaponDamageMult().modifyPercent(SRC, dmgBonus);
                stats.getEnergyWeaponDamageMult().modifyPercent(SRC, dmgBonus);
                stats.getMissileWeaponDamageMult().modifyPercent(SRC, dmgBonus);
            }
        }
    }
}
