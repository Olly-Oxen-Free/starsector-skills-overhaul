package skills_overhaul.barevents;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

import java.util.Map;

public class SeasonedCrewBarEvent extends BaseBarEventCreator {

    public static final String EVENT_ID = "so_seasoned_crew";
    private static final long COST = 10000L;
    private static final String OPT_ACCEPT = "so_seasoned_accept";
    private static final String OPT_DECLINE = "so_seasoned_decline";

    @Override
    public PortsideBarEvent createBarEvent() {
        return new SeasonedCrewDialog();
    }

    @Override public String getBarEventId() { return EVENT_ID; }
    @Override public float getBarEventFrequencyWeight() { return 0.2f; }
    @Override public float getBarEventTimeoutDuration() { return 120f; }
    @Override public float getBarEventAcceptedTimeoutDuration() { return 360f; }

    static final class SeasonedCrewDialog extends BaseBarEventWithPerson {

        /**
         * Bar-menu entry. The option data must be the event itself: BarCMD only routes options whose
         * data is a PortsideBarEvent to the event; String data went to the rules engine and the
         * accept/decline options previously added here did nothing.
         */
        @Override
        public void addPromptAndOption(InteractionDialogAPI dialog,
                                       Map<String, com.fs.starfarer.api.campaign.rules.MemoryAPI> memoryMap) {
            super.addPromptAndOption(dialog, memoryMap);
            TextPanelAPI text = dialog.getTextPanel();
            text.addPara("A grizzled spacer nurses a drink alone at the end of the bar.");
            dialog.getOptionPanel().addOption("Talk to the grizzled spacer", this);
        }

        @Override
        public void init(InteractionDialogAPI dialog,
                         Map<String, com.fs.starfarer.api.campaign.rules.MemoryAPI> memoryMap) {
            super.init(dialog, memoryMap);
            done = false;
            dialog.getTextPanel().addPara("The spacer catches your eye. \"I've flown more tonnage than most "
                    + "admirals. Teach you what I know — for the right price.\"");
            OptionPanelAPI opts = dialog.getOptionPanel();
            opts.clearOptions();
            opts.addOption("Pay " + COST + " credits for their expertise (+1 Pilot point)", OPT_ACCEPT);
            opts.addOption("Politely decline", OPT_DECLINE);
            long credits = (long) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
            if (credits < COST) {
                opts.setEnabled(OPT_ACCEPT, false);
                opts.setTooltip(OPT_ACCEPT, "Insufficient credits.");
            }
        }

        @Override
        public void optionSelected(String text, Object optionData) {
            if (OPT_ACCEPT.equals(optionData)) {
                Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(COST);
                SkillMemory.addPoints(Global.getSector().getMemoryWithoutUpdate(), Tree.PILOT, 1);
                dialog.getTextPanel().addPara(
                        "You spend a few hours with the veteran. Something clicks. (+1 Pilot point)");
                // Remove from the bar + start the accepted timeout; otherwise the event stayed
                // and could be bought repeatedly (+1 point per 10k credits, unbounded).
                BarEventManager.getInstance().notifyWasInteractedWith(this);
                done = true;
                noContinue = false;
            } else {
                done = true;
                noContinue = false;
            }
        }

        @Override
        public boolean shouldShowAtMarket(MarketAPI market) {
            return market != null && market.isInEconomy();
        }
    }
}
