package skills_overhaul.xp;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.util.HashMap;
import java.util.Map;
import skills_overhaul.config.ConfigLoader;
import skills_overhaul.integration.NexIntegration;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;

public class AdminXPTracker implements EconomyTickListener, ColonyInteractionListener {

    public static final String FOUNDING_N_KEY = "$skills.admin.founding_n";
    public static final String STREAK_PREFIX  = "$skills.admin.streak.";
    public static final float  BASE_FOUNDING_XP = 1000f;
    public static final float  MILITARY_BONUS   = 500f;
    public static final float  INCOME_RATE      = 2.0f;
    public static final float  STREAK_BASE      = 10.0f;

    public static final Map<String, Float> CRISIS_VALUES = new HashMap<String, Float>();
    static {
        CRISIS_VALUES.put("luddic_path_cell", 500f);
        CRISIS_VALUES.put("pirate_base",       400f);
        CRISIS_VALUES.put("hegemony_inspection", 600f);
    }

    public void onColonyFounded(MarketAPI market) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int n = (int) mem.getFloat(FOUNDING_N_KEY);
        float xp = AdminXPMath.foundingXP(n, BASE_FOUNDING_XP)
                 * AdminXPMath.modifier(isSelfAdmin(market), isVassal(market),
                         ConfigLoader.get().vassalXpMult,
                         ConfigLoader.get().administratorXpMult);
        SkillMemory.addXP(mem, Tree.ADMIN, xp);
        mem.set(FOUNDING_N_KEY, (float)(n + 1));
    }

    public void onMarketConquered(MarketAPI market) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        int n = (int) mem.getFloat(FOUNDING_N_KEY);
        float xp = AdminXPMath.conquestXP(n, market.getSize(), MILITARY_BONUS, BASE_FOUNDING_XP)
                 * AdminXPMath.modifier(isSelfAdmin(market), isVassal(market),
                         ConfigLoader.get().vassalXpMult,
                         ConfigLoader.get().administratorXpMult);
        SkillMemory.addXP(mem, Tree.ADMIN, xp);
        mem.set(FOUNDING_N_KEY, (float)(n + 1));
    }

    public void onCrisisResolved(String crisisType, MarketAPI market) {
        Float value = CRISIS_VALUES.get(crisisType);
        if (value == null) return;
        MemoryAPI mem = market.getMemoryWithoutUpdate();
        String cdKey = "$skills.admin.crisis_cd." + crisisType;
        if (mem.getBoolean(cdKey)) return;
        mem.set(cdKey, true, 365f);
        float xp = value * AdminXPMath.modifier(isSelfAdmin(market), isVassal(market),
                      ConfigLoader.get().vassalXpMult,
                      ConfigLoader.get().administratorXpMult);
        SkillMemory.addXP(Global.getSector().getMemoryWithoutUpdate(), Tree.ADMIN, xp);
    }

    @Override
    public void reportEconomyMonthEnd() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        // A5 fix: clear streak for any market that is no longer player-owned.
        // Otherwise a re-conquest would start at the prior streak value (≤6
        // bounded by streakXP cap, but still allows instant-max farm).
        for (String k : new java.util.ArrayList<String>(mem.getKeys())) {
            if (!k.startsWith(STREAK_PREFIX)) continue;
            String marketId = k.substring(STREAK_PREFIX.length());
            MarketAPI m = Global.getSector().getEconomy().getMarket(marketId);
            if (m == null || (!m.getFaction().isPlayerFaction() && !isVassal(m))) {
                mem.unset(k);
            }
        }
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!m.getFaction().isPlayerFaction() && !isVassal(m)) continue;

            float monthNet = m.getNetIncome();
            float xp = AdminXPMath.incomeXP(monthNet, INCOME_RATE)
                     * AdminXPMath.modifier(isSelfAdmin(m), isVassal(m),
                             ConfigLoader.get().vassalXpMult,
                             ConfigLoader.get().administratorXpMult);

            int prev = (int) mem.getFloat(STREAK_PREFIX + m.getId());
            int now  = AdminXPMath.updateStreak(prev, m.getStability().getModifiedInt(),
                                                ConfigLoader.get().goodStandingThreshold);
            mem.set(STREAK_PREFIX + m.getId(), (float) now);
            xp += AdminXPMath.streakXP(now, ConfigLoader.get().stabilityStreakCapMonths, STREAK_BASE)
                * AdminXPMath.modifier(isSelfAdmin(m), isVassal(m),
                         ConfigLoader.get().vassalXpMult,
                         ConfigLoader.get().administratorXpMult);

            SkillMemory.addXP(mem, Tree.ADMIN, xp);
        }
    }

    @Override public void reportEconomyTick(int iterIndex) {
        if (iterIndex != 0) return;
        // A3 fix: poll Nex conquests BEFORE the colony-found loop. The
        // conquest poll marks captured markets so they go down the conquest
        // path; the founding loop only catches genuinely new colonies.
        NexIntegration.pollForConquests(this);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!m.getFaction().isPlayerFaction()) continue;
            String seenKey = "$skills.admin.colony_seen." + m.getId();
            if (!mem.getBoolean(seenKey)) {
                mem.set(seenKey, true);
                onColonyFounded(m);
            }
        }
    }

    @Override public void reportPlayerOpenedMarket(MarketAPI m) {}
    @Override public void reportPlayerClosedMarket(MarketAPI m) {}
    @Override public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI m) {}
    @Override public void reportPlayerMarketTransaction(PlayerMarketTransaction t) {}

    private boolean isSelfAdmin(MarketAPI m) {
        return m.getAdmin() != null && m.getAdmin() == Global.getSector().getPlayerPerson();
    }
    private boolean isVassal(MarketAPI m) {
        return m.getMemoryWithoutUpdate().getBoolean("$vassal");
    }
}
