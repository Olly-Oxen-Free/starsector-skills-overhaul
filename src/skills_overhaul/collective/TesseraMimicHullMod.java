package skills_overhaul.collective;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 * Tessera Mimic — machine-fragment hullmod. STUB — full implementation deferred.
 * On ship destruction: spawns 2–4 bio-mechanical fragments for the battle remainder.
 * See spec §Lane 14 "Orphaned concept from Abyss."
 */
public class TesseraMimicHullMod extends BaseHullMod {

    public static final String ID = "so_tessera_mimic";

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}
