package skills_overhaul.artificer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Live consumer for {@link AutomatedShipPointService}. Turns the bookkeeping
 * "raised cap" into an actual in-game cap raise for the player's automated ships.
 *
 * <h3>Why not a character dynamic stat</h3>
 * The audit note proposed modifying {@code MutableCharacterStatsAPI.getDynamic()
 * .getMod(Stats.AUTOMATED_POINTS)}. That constant does not exist in this game
 * version, and there is no skill-independent dynamic stat that the engine reads
 * to size the automated-ship allowance. The allowance is the static
 * {@code BaseSkillEffectDescription.AUTOMATED_POINTS_THRESHOLD} (= 120, matching
 * {@link AutomatedShipPointService#VANILLA_BASE}), compared against the fleet's
 * summed automated-ship points computed by {@code getAutomatedPoints(...)}.
 *
 * The only public lever the vanilla point-summing logic honours is the captain
 * memory key {@code $autoPointsMult}, read per-member and multiplied into that
 * member's contribution. Scaling every automated ship's contribution down by
 * {@code VANILLA_BASE / raisedCap} makes the fixed 120 threshold behave like the
 * raised cap (e.g. raisedCap 160 → mult 0.75 → the fleet fits ~33% more automated
 * points under the same threshold). This is the standard, reversible, public-API
 * mechanism; it is independent of which specific skills the player owns.
 */
public final class AutomatedShipCapApplier {

    /** Captain memory key the vanilla point-summing logic multiplies per member. */
    static final String AUTO_POINTS_MULT_KEY = "$autoPointsMult";

    /**
     * Effective per-ship contribution multiplier that makes the fixed vanilla
     * threshold behave like {@link AutomatedShipPointService#getRaisedCap()}.
     * Returns 1.0 when no bonus is active (raised cap == vanilla base).
     */
    public static float getEffectiveMult() {
        int raised = AutomatedShipPointService.getRaisedCap();
        if (raised <= AutomatedShipPointService.VANILLA_BASE) return 1f;
        return (float) AutomatedShipPointService.VANILLA_BASE / (float) raised;
    }

    /**
     * Re-stamp the effective multiplier onto every automated ship captain in the
     * player fleet (or clear it when no bonus is active). Safe to call repeatedly;
     * invoked at node purchase/removal and on load via the effect handler.
     */
    public static void applyToPlayerFleet() {
        CampaignFleetAPI fleet = Global.getSector() != null ? Global.getSector().getPlayerFleet() : null;
        if (fleet == null) return;

        float mult = getEffectiveMult();
        boolean active = mult < 1f;

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member == null || !Misc.isAutomated(member)) continue;
            PersonAPI captain = member.getCaptain();
            if (captain == null) continue;
            MemoryAPI mem = captain.getMemoryWithoutUpdate();
            if (mem == null) continue;
            if (active) {
                mem.set(AUTO_POINTS_MULT_KEY, mult);
            } else {
                mem.unset(AUTO_POINTS_MULT_KEY);
            }
        }
    }

    /**
     * Sum the player fleet's counted automated-ship points (deployment-point cost
     * per non-mothballed automated member). Used with {@link #overCapForPlayerFleet()}.
     */
    public static int countPlayerAutomatedPoints() {
        CampaignFleetAPI fleet = Global.getSector() != null ? Global.getSector().getPlayerFleet() : null;
        if (fleet == null) return 0;
        float total = 0f;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member == null || member.isMothballed() || !Misc.isAutomated(member)) continue;
            total += member.getDeploymentPointsCost();
        }
        return Math.round(total);
    }

    /** Points by which the player fleet exceeds the raised cap (0 if within cap). */
    public static int overCapForPlayerFleet() {
        return AutomatedShipPointService.computeOverCap(
                countPlayerAutomatedPoints(), AutomatedShipPointService.getRaisedCap());
    }

    private AutomatedShipCapApplier() {}
}
