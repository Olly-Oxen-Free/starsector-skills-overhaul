package skills_overhaul.supply;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignUIRenderingListener;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.lwjgl.opengl.GL11;
import skills_overhaul.state.SkillMemory;

public final class SupplyBufferRenderer implements EveryFrameScript, CampaignUIRenderingListener {

    private boolean listenerRegistered = false;

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }

    @Override
    public void advance(float amount) {
        if (!listenerRegistered) {
            Global.getSector().getListenerManager().addListener(this, true);
            listenerRegistered = true;
        }
    }

    @Override
    public void renderInUICoordsBelowUI(ViewportAPI viewport) {
        drawBar(viewport);
    }

    @Override
    public void renderInUICoordsAboveUIBelowTooltips(ViewportAPI viewport) {}

    @Override
    public void renderInUICoordsAboveUIAndTooltips(ViewportAPI viewport) {}

    private void drawBar(ViewportAPI viewport) {
        float value = currentValue();
        float cap = currentCap();
        if (cap <= 0f) return;

        float fraction = Math.min(1f, value / cap);
        float barW = 120f;
        float barH = 10f;
        float x = viewport.getVisibleWidth() - barW - 20f;
        float y = 60f;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glColor4f(0.1f, 0.1f, 0.1f, 0.6f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + barW, y);
        GL11.glVertex2f(x + barW, y + barH);
        GL11.glVertex2f(x, y + barH);
        GL11.glEnd();

        if (fraction > 0f) {
            GL11.glColor4f(0.85f, 0.65f, 0.1f, 0.8f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + barW * fraction, y);
            GL11.glVertex2f(x + barW * fraction, y + barH);
            GL11.glVertex2f(x, y + barH);
            GL11.glEnd();
        }

        GL11.glPopAttrib();
    }

    public static float currentValue() {
        return SkillMemory.getSupplyBuffer(mem());
    }

    public static float currentCap() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        float fleetCap = (fleet != null && fleet.getCargo() != null)
                ? fleet.getCargo().getMaxCapacity() : 0f;
        return SupplyBufferService.computeCap(mem(), fleetCap);
    }

    private static MemoryAPI mem() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
