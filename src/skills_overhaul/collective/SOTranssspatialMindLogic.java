package skills_overhaul.collective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

public final class SOTranssspatialMindLogic {

    private static final String SOURCE = "so_transspatial_mind";

    public static void applyCharacterStats() {
        try {
            MutableCharacterStatsAPI stats = Global.getSector().getPlayerPerson().getStats();
            // Forbid human officers
            stats.getOfficerNumber().modifyFlat(SOURCE, -999f);
            // +90 automated ship points
            stats.getDynamic().getMod("so_automated_ship_points").modifyFlat(SOURCE, 90f);
            // -25% AI core ship point cost (if stat key exists)
            stats.getDynamic().getMod("so_automated_ship_points_cost_mult").modifyMult(SOURCE, 0.75f);
        } catch (Exception ignored) {}
    }

    public static void removeCharacterStats() {
        try {
            MutableCharacterStatsAPI stats = Global.getSector().getPlayerPerson().getStats();
            stats.getOfficerNumber().unmodify(SOURCE);
            stats.getDynamic().getMod("so_automated_ship_points").unmodify(SOURCE);
            stats.getDynamic().getMod("so_automated_ship_points_cost_mult").unmodify(SOURCE);
        } catch (Exception ignored) {}
    }

    private SOTranssspatialMindLogic() {}
}
