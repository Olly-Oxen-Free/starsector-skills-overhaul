package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.Collection;

/** Counts d-mods by the hull mod spec's "dmod" tag, as SODefenseDerelictFortHandler does. */
public final class DModCounter {

    /** Lookup seam so the counting can be tested without the game. */
    interface DModCheck { boolean isDMod(String hullModId); }

    static final DModCheck BY_TAG = new DModCheck() {
        @Override
        public boolean isDMod(String hullModId) {
            try {
                HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
                return spec != null && spec.hasTag("dmod");
            } catch (Exception ignored) {
                return false;
            }
        }
    };

    public static int count(Collection<String> hullMods) {
        return count(hullMods, BY_TAG);
    }

    static int count(Collection<String> hullMods, DModCheck check) {
        int c = 0;
        if (hullMods == null) return 0;
        for (String h : hullMods) {
            if (check.isDMod(h)) c++;
        }
        return c;
    }

    private DModCounter() {}
}
