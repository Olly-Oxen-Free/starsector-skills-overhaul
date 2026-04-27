package skills_overhaul.zealot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;

/**
 * Stub for NPC faction reactions to the player declaring a Crusade.
 * Full implementation requires Nexerelin DiplomacyManager review.
 * When Nex is present, this logs the intended declaration and no-ops pending the Nex impl.
 */
public final class CrusadeNPCStub extends BaseCampaignEventListener {

    public CrusadeNPCStub() { super(false); }

    public boolean isNexLoaded() {
        return Global.getSettings().getModManager().isModEnabled("nexerelin");
    }

    /** Call after the player declares a Crusade to trigger NPC sympathizer reactions. */
    public void onPlayerCrusadeDeclared(String playerRivalFactionId) {
        if (!isNexLoaded()) return;
        for (String npcFactionId : new String[]{"hegemony", "luddic_church"}) {
            FactionAPI npc = Global.getSector().getFaction(npcFactionId);
            FactionAPI rival = Global.getSector().getFaction(playerRivalFactionId);
            if (npc == null || rival == null) continue;
            if (npc.getRelationship(rival.getId()) >= 0f) continue;
            tryNexWarDeclaration(npcFactionId, playerRivalFactionId);
        }
    }

    private void tryNexWarDeclaration(String aggressorFaction, String targetFaction) {
        try {
            Class<?> dm = Class.forName("exerelin.campaign.DiplomacyManager");
            com.fs.starfarer.api.campaign.FactionAPI faction1 =
                    Global.getSector().getFaction(aggressorFaction);
            com.fs.starfarer.api.campaign.FactionAPI faction2 =
                    Global.getSector().getFaction(targetFaction);
            if (faction1 == null || faction2 == null) return;

            // adjustRelations(FactionAPI, FactionAPI, float, RepLevel, RepLevel, RepLevel)
            // Sets a delta on their relationship clamped to the given RepLevel bounds.
            // We pass -1.0f (full hostile) with null bounds to force HOSTILE.
            java.lang.reflect.Method adjustRelations = null;
            for (java.lang.reflect.Method m : dm.getDeclaredMethods()) {
                if (m.getName().equals("adjustRelations")
                        && m.getParameterCount() == 6
                        && m.getParameterTypes()[0].equals(com.fs.starfarer.api.campaign.FactionAPI.class)
                        && m.getParameterTypes()[2].equals(float.class)) {
                    adjustRelations = m;
                    break;
                }
            }
            if (adjustRelations != null) {
                adjustRelations.invoke(null, faction1, faction2, -1.0f, null, null, null);
                Global.getLogger(CrusadeNPCStub.class).info(
                        "[Zealot] NPC Crusade: " + aggressorFaction + " adjusted hostile to " + targetFaction
                        + " via Nex DiplomacyManager.adjustRelations");
            } else {
                Global.getLogger(CrusadeNPCStub.class).info(
                        "[Zealot] NPC Crusade: adjustRelations(6-param) not found in Nex DiplomacyManager; no-op");
            }
        } catch (ClassNotFoundException ex) {
            Global.getLogger(CrusadeNPCStub.class).info(
                    "[Zealot] NPC Crusade: Nex not loaded, no-op");
        } catch (Throwable t) {
            Global.getLogger(CrusadeNPCStub.class).warn(
                    "[Zealot] NPC Crusade reflection failed: " + t.getMessage());
        }
    }
}
