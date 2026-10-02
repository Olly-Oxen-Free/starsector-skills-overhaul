package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

public final class SOTranssspatialMindLogic {

    private static final String SOURCE = "so_transspatial_mind";

    public static void applyCharacterStats() {
        try {
            MutableCharacterStatsAPI stats = Global.getSector().getPlayerPerson().getStats();
            // Forbid human officers
            // Prevents new officer assignment; does not evict officers already in slots
            stats.getOfficerNumber().modifyFlat(SOURCE, -999f);
            // +90 automated ship points ("so_automated_ship_points" is read by nothing; the live
            // cap is AutomatedShipPointService + AutomatedShipCapApplier)
            skills_overhaul.artificer.AutomatedShipPointService.registerBonus(SOURCE, 90);
            skills_overhaul.artificer.AutomatedShipCapApplier.applyToPlayerFleet();
            // -25% AI core ship point cost (if stat key exists)
            stats.getDynamic().getMod("so_automated_ship_points_cost_mult").modifyMult(SOURCE, 0.75f);
        } catch (Exception ignored) {}
    }

    public static void removeCharacterStats() {
        try {
            MutableCharacterStatsAPI stats = Global.getSector().getPlayerPerson().getStats();
            stats.getOfficerNumber().unmodify(SOURCE);
            skills_overhaul.artificer.AutomatedShipPointService.unregisterBonus(SOURCE);
            skills_overhaul.artificer.AutomatedShipCapApplier.applyToPlayerFleet();
            stats.getDynamic().getMod("so_automated_ship_points_cost_mult").unmodify(SOURCE);
        } catch (Exception ignored) {}
    }

    private SOTranssspatialMindLogic() {}
}
