package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;

public final class SOCommandHierarchyLogic {

    private static final String SRC         = "so_cmd_hierarchy_aura";
    private static final float  AURA        = 2000f;
    private static final String POINT_SOURCE = "so_command_hierarchy";

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_command_hierarchy")) return;

        // 1. Gather all alive Alpha-cored player ships with their bonus
        List<ShipAPI> alphas = new ArrayList<>();
        for (ShipAPI s : engine.getShips()) {
            if (s.getOwner() != 0 || !s.isAlive() || !isAlphaCore(s)) continue;
            alphas.add(s);
        }

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        MemoryView view = new MemoryAPIView(mem);

        // 2. For each automated player ship, compute max aura bonus from any alpha in range
        for (ShipAPI target : engine.getShips()) {
            if (target.getOwner() != 0 || !target.isAlive() || !isAICored(target)) continue;

            float maxBonus = 0f;
            for (ShipAPI alpha : alphas) {
                if (alpha == target) continue;
                float dist = Misc.getDistance(alpha.getLocation(), target.getLocation());
                if (dist <= AURA) {
                    String coreId = alpha.getCaptain().getId();
                    KnowledgeTier tier = KnowledgeTracker.getTier(view, coreId);
                    float bonus = tier.getAuraBonusPct();
                    if (bonus > maxBonus) maxBonus = bonus;
                }
            }

            MutableShipStatsAPI stats = target.getMutableStats();
            if (maxBonus <= 0f) {
                stats.getBallisticWeaponDamageMult().unmodify(SRC);
                stats.getEnergyWeaponDamageMult().unmodify(SRC);
            } else {
                stats.getBallisticWeaponDamageMult().modifyPercent(SRC, maxBonus);
                stats.getEnergyWeaponDamageMult().modifyPercent(SRC, maxBonus);
            }
        }
    }

    private static boolean isAlphaCore(ShipAPI ship) {
        return ship.getCaptain() != null
                && ship.getCaptain().isAICore()
                && "alpha_core".equals(ship.getCaptain().getAICoreId());
    }

    private static boolean isAICored(ShipAPI ship) {
        return ship.getCaptain() != null && ship.getCaptain().isAICore();
    }

    // "so_automated_ship_points" is not read by anything; the live automated-points cap is
    // AutomatedShipPointService + AutomatedShipCapApplier.
    public static void applyPointBonus() {
        skills_overhaul.artificer.AutomatedShipPointService.registerBonus(POINT_SOURCE, 40);
        skills_overhaul.artificer.AutomatedShipCapApplier.applyToPlayerFleet();
    }

    public static void removePointBonus() {
        skills_overhaul.artificer.AutomatedShipPointService.unregisterBonus(POINT_SOURCE);
        skills_overhaul.artificer.AutomatedShipCapApplier.applyToPlayerFleet();
    }
}
