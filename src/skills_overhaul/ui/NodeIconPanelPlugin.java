package skills_overhaul.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Per-node visual chrome — a vanilla-style framed skill cell.
 *
 * Rendered layers (bottom → top):
 *   1. dark backing plate (flat, slight gradient)
 *   2. 1px outer frame in state color
 *   3. 1px inner highlight (lighter shade of state color)
 *   4. small corner brackets for "tech" feel
 *
 * The icon image itself is added as a child TooltipMakerAPI image by the
 * builder and so sits on top of this chrome.
 */
public final class NodeIconPanelPlugin extends BaseCustomUIPanelPlugin {

    private final Color borderColor;
    private final boolean owned;
    private final boolean pending;
    private final boolean unlockable;
    private final boolean capstone;
    private PositionAPI pos;

    public NodeIconPanelPlugin(Color borderColor, boolean owned, boolean pending,
                               boolean unlockable, boolean capstone) {
        this.borderColor = borderColor;
        this.owned = owned;
        this.pending = pending;
        this.unlockable = unlockable;
        this.capstone = capstone;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.pos = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (pos == null) return;
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Backing plate — top slightly brighter than bottom to read as an inset cell.
        float plateTopA, plateBotA;
        if (owned)          { plateTopA = 0.42f; plateBotA = 0.22f; }
        else if (pending)   { plateTopA = 0.36f; plateBotA = 0.18f; }
        else if (unlockable){ plateTopA = 0.30f; plateBotA = 0.14f; }
        else                { plateTopA = 0.22f; plateBotA = 0.10f; }

        float pr = borderColor.getRed()   / 255f * 0.35f;
        float pg = borderColor.getGreen() / 255f * 0.35f;
        float pb = borderColor.getBlue()  / 255f * 0.35f;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(pr, pg, pb, plateTopA * alphaMult);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glColor4f(pr * 0.55f, pg * 0.55f, pb * 0.55f, plateBotA * alphaMult);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();

        GL11.glPopAttrib();
    }

    @Override
    public void render(float alphaMult) {
        if (pos == null) return;
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float r = borderColor.getRed()   / 255f;
        float g = borderColor.getGreen() / 255f;
        float b = borderColor.getBlue()  / 255f;
        float baseA = (borderColor.getAlpha() / 255f) * alphaMult;
        float frameA = owned ? baseA : (pending ? baseA * 0.95f : (unlockable ? baseA * 0.85f : baseA * 0.55f));

        // Outer 1px frame.
        GL11.glLineWidth(1.0f);
        GL11.glColor4f(r, g, b, frameA);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x + 0.5f,     y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + h - 0.5f);
        GL11.glVertex2f(x + 0.5f,     y + h - 0.5f);
        GL11.glEnd();

        // Inner 1px highlight (slightly lighter shade).
        float hr = Math.min(1f, r + 0.25f);
        float hg = Math.min(1f, g + 0.25f);
        float hb = Math.min(1f, b + 0.25f);
        GL11.glColor4f(hr, hg, hb, frameA * 0.45f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x + 1.5f,     y + 1.5f);
        GL11.glVertex2f(x + w - 1.5f, y + 1.5f);
        GL11.glVertex2f(x + w - 1.5f, y + h - 1.5f);
        GL11.glVertex2f(x + 1.5f,     y + h - 1.5f);
        GL11.glEnd();

        // Corner brackets (slightly longer for capstones).
        float c = capstone ? 12f : 7f;
        GL11.glLineWidth(1.6f);
        GL11.glColor4f(r, g, b, frameA);
        GL11.glBegin(GL11.GL_LINES);
        // TL
        GL11.glVertex2f(x + 0.5f, y + c);     GL11.glVertex2f(x + 0.5f, y + 0.5f);
        GL11.glVertex2f(x + 0.5f, y + 0.5f);  GL11.glVertex2f(x + c,    y + 0.5f);
        // TR
        GL11.glVertex2f(x + w - c,   y + 0.5f);     GL11.glVertex2f(x + w - 0.5f, y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + 0.5f);    GL11.glVertex2f(x + w - 0.5f, y + c);
        // BR
        GL11.glVertex2f(x + w - 0.5f, y + h - c);     GL11.glVertex2f(x + w - 0.5f, y + h - 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + h - 0.5f);  GL11.glVertex2f(x + w - c,    y + h - 0.5f);
        // BL
        GL11.glVertex2f(x + c,    y + h - 0.5f);  GL11.glVertex2f(x + 0.5f, y + h - 0.5f);
        GL11.glVertex2f(x + 0.5f, y + h - 0.5f);  GL11.glVertex2f(x + 0.5f, y + h - c);
        GL11.glEnd();

        GL11.glPopAttrib();
    }
}
