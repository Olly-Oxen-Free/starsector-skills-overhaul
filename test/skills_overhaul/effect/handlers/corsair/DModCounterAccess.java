package skills_overhaul.effect.handlers.corsair;

import java.util.Collection;
import java.util.Set;

/** Test-only bridge to DModCounter's package-private lookup seam. */
public final class DModCounterAccess {
    public static int count(Collection<String> hullMods, final Set<String> dmodTagged) {
        return DModCounter.count(hullMods, new DModCounter.DModCheck() {
            @Override
            public boolean isDMod(String hullModId) { return dmodTagged.contains(hullModId); }
        });
    }

    private DModCounterAccess() {}
}
