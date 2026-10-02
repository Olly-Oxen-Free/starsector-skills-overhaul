package skills_overhaul.corsair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Escort AI for the Letter-of-Marque privateer fleet.
 *
 * Behavior:
 *  - Stay within FOLLOW_RADIUS_SU of the player while idle.
 *  - If a hostile fleet is within ENGAGE_RADIUS_SU of either player or self → intercept.
 *  - If the player jumps system → follow on a JUMP_FOLLOW_DELAY_DAYS delay.
 *
 * Uses EveryFrameScript (added via fleet.addScript) since FleetAdvanceScript
 * is a concrete class in this API version, not an interface.
 *
 * Adapted (no code copied) from IndEvo splitfleet.fleetAssignmentAIs.EscortAssignmentAI.
 */
public final class PrivateerEscortAI implements EveryFrameScript {

    public static final float FOLLOW_RADIUS_SU   = 1500f;
    public static final float ENGAGE_RADIUS_SU   = 3000f;
    public static final float JUMP_FOLLOW_DELAY_DAYS = 1f;

    private final CampaignFleetAPI privateer;
    private float jumpDelayElapsed = 0f;
    private boolean done = false;

    public PrivateerEscortAI(CampaignFleetAPI privateer) {
        this.privateer = privateer;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (privateer == null || !privateer.isAlive()) {
            done = true;
            return;
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;

        // Player jumped to a different system — queue follow with delay
        if (player.getContainingLocation() != privateer.getContainingLocation()) {
            jumpDelayElapsed += Misc.getDays(amount);
            if (jumpDelayElapsed >= JUMP_FOLLOW_DELAY_DAYS) {
                assign(FleetAssignment.GO_TO_LOCATION, player, 1f);
                jumpDelayElapsed = 0f;
            }
            return;
        }

        jumpDelayElapsed = 0f;

        // Engage nearby hostiles
        CampaignFleetAPI target = findHostileTarget(player);
        if (target != null) {
            assign(FleetAssignment.INTERCEPT, target, 0.5f);
            return;
        }

        // Stay close to player
        float dist = Misc.getDistance(privateer.getLocation(), player.getLocation());
        if (dist > FOLLOW_RADIUS_SU) {
            assign(FleetAssignment.GO_TO_LOCATION, player, 0.5f);
        }
    }

    /**
     * Replace the current assignment only when it differs. Clearing and re-adding every frame
     * reset the fleet AI's pathing each tick.
     */
    private void assign(FleetAssignment type, SectorEntityToken target, float days) {
        if (privateer.getAI() != null) {
            FleetAssignmentDataAPI cur = privateer.getAI().getCurrentAssignment();
            if (cur != null && cur.getAssignment() == type && cur.getTarget() == target) return;
        }
        privateer.clearAssignments();
        privateer.addAssignment(type, target, days);
    }

    private CampaignFleetAPI findHostileTarget(CampaignFleetAPI player) {
        for (CampaignFleetAPI f : privateer.getContainingLocation().getFleets()) {
            if (f == privateer || f == player) continue;
            if (f.isStationMode()) continue; // never send the escort into a station
            if (!f.getFaction().isHostileTo(player.getFaction())) continue;
            float distFromSelf   = Misc.getDistance(privateer.getLocation(), f.getLocation());
            float distFromPlayer = Misc.getDistance(player.getLocation(),    f.getLocation());
            if (distFromSelf < ENGAGE_RADIUS_SU || distFromPlayer < ENGAGE_RADIUS_SU) {
                return f;
            }
        }
        return null;
    }
}
