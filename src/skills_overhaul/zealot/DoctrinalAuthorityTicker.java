package skills_overhaul.zealot;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

/**
 * v1 scaffold: tracks player commission faction. Per-tick faction-fleet buff propagation
 * is logged but not applied (concrete fleet-stat application TBD).
 */
public final class DoctrinalAuthorityTicker implements EveryFrameScript {

    public static final String STAT_KEY = "skills_overhaul_doctrinal_authority";
    public static final String K_COMMISSION_LAST = "$skills.zealot.doctrinal_authority.commission_last";

    private final IntervalUtil interval = new IntervalUtil(5f, 5f);

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        if (!Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate()
                .get(skills_overhaul.effect.handlers.SODoctrinalAuthorityHandler.K_PURCHASED))) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        String commissionId = null;
        try {
            commissionId = Misc.getCommissionFactionId();
        } catch (Throwable t) {
            // Misc.getCommissionFactionId may not exist on all versions
        }

        Object lastObj = Global.getSector().getMemoryWithoutUpdate().get(K_COMMISSION_LAST);
        String last = lastObj instanceof String ? (String) lastObj : null;
        if (commissionId != null && !commissionId.equals(last)) {
            Global.getLogger(DoctrinalAuthorityTicker.class).info(
                    "[Zealot] Doctrinal Authority: commission detected -> " + commissionId);
            Global.getSector().getMemoryWithoutUpdate().set(K_COMMISSION_LAST, commissionId);
        } else if (commissionId == null && last != null) {
            Global.getLogger(DoctrinalAuthorityTicker.class).info(
                    "[Zealot] Doctrinal Authority: commission lost");
            Global.getSector().getMemoryWithoutUpdate().unset(K_COMMISSION_LAST);
        }

        // TODO: per-fleet buff propagation requires fleet-stat key for "weapon damage" via dynamic bonus.
    }
}
