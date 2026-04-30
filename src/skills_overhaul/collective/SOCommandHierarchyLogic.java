package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

public final class SOCommandHierarchyLogic {

    private static final String SRC         = "so_cmd_hierarchy_aura";
    private static final float  AURA        = 2000f;
    private static final String POINT_SOURCE = "so_command_hierarchy";

    public void advance(CombatEngineAPI engine) {
        if (!CollectiveActiveNodes.isActive("so_collective_command_hierarchy")) return;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        MemoryView view = new MemoryAPIView(mem);

        for (ShipAPI source : engine.getShips()) {
            if (source.getOwner() != 0 || !source.isAlive()) continue;
            if (!isAlphaCore(source)) continue;

            String coreId = source.getCaptain().getId();
            KnowledgeTier tier = KnowledgeTracker.getTier(view, coreId);
            float bonusPct = tier.getAuraBonusPct();
            if (bonusPct <= 0f) continue;

            for (ShipAPI ship : engine.getShips()) {
                if (ship.getOwner() != 0 || !ship.isAlive()) continue;
                if (!isAICored(ship)) continue;

                MutableShipStatsAPI stats = ship.getMutableStats();
                float dist = Misc.getDistance(source.getLocation(), ship.getLocation());
                if (dist <= AURA) {
                    stats.getBallisticWeaponDamageMult().modifyPercent(SRC, bonusPct);
                    stats.getEnergyWeaponDamageMult().modifyPercent(SRC, bonusPct);
                } else {
                    stats.getBallisticWeaponDamageMult().unmodify(SRC);
                    stats.getEnergyWeaponDamageMult().unmodify(SRC);
                }
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

    public static void applyPointBonus() {
        try {
            Global.getSector().getPlayerPerson()
                .getStats().getDynamic().getMod("so_automated_ship_points")
                .modifyFlat(POINT_SOURCE, 40f);
        } catch (Exception ignored) {}
    }

    public static void removePointBonus() {
        try {
            Global.getSector().getPlayerPerson()
                .getStats().getDynamic().getMod("so_automated_ship_points")
                .unmodify(POINT_SOURCE);
        } catch (Exception ignored) {}
    }
}
