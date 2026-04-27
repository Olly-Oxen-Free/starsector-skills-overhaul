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
            Class.forName("exerelin.campaign.DiplomacyManager");
            Global.getLogger(CrusadeNPCStub.class).info(
                    "[Zealot] NPC Crusade stub: " + aggressorFaction + " -> " + targetFaction
                    + " (Nex DiplomacyManager found; declaration deferred to Nex impl review)");
        } catch (ClassNotFoundException ex) {
            Global.getLogger(CrusadeNPCStub.class).info(
                    "[Zealot] NPC Crusade stub: Nex DiplomacyManager not found, no-op");
        }
    }
}
