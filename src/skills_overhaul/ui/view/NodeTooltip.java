package skills_overhaul.ui.view;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;

import java.awt.Color;

/** Thin TooltipCreator that renders a {@link NodeTooltipContent}. */
public final class NodeTooltip implements TooltipCreator {

    public interface Source { NodeTooltipContent get(); }

    public static final float WIDTH = 300f;

    private final Source source;

    public NodeTooltip(NodeTooltipContent content) {
        final NodeTooltipContent c = content;
        this.source = new Source() { public NodeTooltipContent get() { return c; } };
    }

    /** Use with a recreate-every-frame tooltip when the text depends on live state. */
    public NodeTooltip(Source source) { this.source = source; }

    @Override public boolean isTooltipExpandable(Object tooltipParam) { return false; }

    @Override public float getTooltipWidth(Object tooltipParam) { return WIDTH; }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        for (NodeTooltipContent.Line l : source.get().getLines()) {
            if (l.style == NodeTooltipContent.Style.TITLE) {
                tooltip.addTitle(l.text);
            } else {
                tooltip.addPara(l.text, color(l.style), 6f);
            }
        }
    }

    private static Color color(NodeTooltipContent.Style s) {
        switch (s) {
            case COST:         return UiColors.PENDING;
            case EFFECT:       return UiColors.UNLOCKABLE;
            case PREREQ_MET:   return UiColors.OWNED;
            case PREREQ_UNMET:
            case BLOCKED:      return Color.RED;
            default:           return Color.WHITE;
        }
    }
}
