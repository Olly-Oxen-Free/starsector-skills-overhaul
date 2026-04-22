package skills_overhaul.integration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import skills_overhaul.xp.AdminXPTracker;

public class NexIntegration {
    public static boolean isLoaded() {
        return Global.getSettings().getModManager().isModEnabled("nexerelin");
    }

    public static void pollForConquests(AdminXPTracker tracker) {
        if (!isLoaded()) return;
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!m.getFaction().isPlayerFaction()) continue;
            String seenKey = "$skills.admin.conquest_seen." + m.getId();
            if (m.getMemoryWithoutUpdate().getBoolean(seenKey)) continue;
            if (!m.getMemoryWithoutUpdate().getBoolean("$skills.admin.colony_seen." + m.getId())) {
                m.getMemoryWithoutUpdate().set(seenKey, true);
                tracker.onMarketConquered(m);
            }
        }
    }
}
