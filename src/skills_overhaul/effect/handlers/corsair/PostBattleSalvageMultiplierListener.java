package skills_overhaul.effect.handlers.corsair;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PostBattleSalvageMultiplierListener extends BaseCampaignEventListener {

    private static final Map<String, Float> MULTIPLIERS = new LinkedHashMap<>();

    public static void register(String key, float mult) { MULTIPLIERS.put(key, mult); }
    public static void unregister(String key)            { MULTIPLIERS.remove(key); }

    public PostBattleSalvageMultiplierListener() { super(false); }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin context, CargoAPI loot) {
        if (loot == null || MULTIPLIERS.isEmpty()) return;
        float total = 1f;
        for (Float m : MULTIPLIERS.values()) total *= m;
        if (total == 1f) return;
        // Inflate every commodity in the loot by (total - 1).
        for (CargoStackAPI st : loot.getStacksCopy()) {
            float bonus = st.getSize() * (total - 1f);
            if (bonus > 0f) loot.addCommodity(st.getCommodityId(), bonus);
        }
    }
}
