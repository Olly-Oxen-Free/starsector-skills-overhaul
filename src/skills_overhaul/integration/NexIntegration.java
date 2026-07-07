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
            String conquestSeenKey = "$skills.admin.conquest_seen." + m.getId();
            String colonySeenKey   = "$skills.admin.colony_seen."   + m.getId();
            if (m.getMemoryWithoutUpdate().getBoolean(conquestSeenKey)) continue;
            // A market we haven't yet credited that wears Nex's captured-by
            // flag is a conquest, not a founding.
            boolean nexMarkedCapture = m.getMemoryWithoutUpdate().contains("$nex_marketCapturedBy");
            if (nexMarkedCapture && !Global.getSector().getMemoryWithoutUpdate().getBoolean(colonySeenKey)) {
                m.getMemoryWithoutUpdate().set(conquestSeenKey, true);
                // Also mark colony_seen so the founding loop in
                // AdminXPTracker.reportEconomyTick skips it (avoids dual award).
                Global.getSector().getMemoryWithoutUpdate().set(colonySeenKey, true);
                tracker.onMarketConquered(m);
            }
        }
    }
}
