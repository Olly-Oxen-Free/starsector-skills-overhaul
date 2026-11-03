package skills_overhaul.corsair;

import java.util.Map;

/**
 * Pure relation rule for the shared privateer faction: it mirrors the player faction's
 * relations exactly, so a hostility the player has ended is undone and a new one is
 * picked up. Kept free of game types so it can be unit-tested.
 */
public final class PrivateerRelations {

    /**
     * Overwrites {@code privateer} so it matches {@code player} for every faction the
     * player has a relation with, is fully friendly to the player, and never holds a
     * relation to itself.
     */
    public static void sync(Map<String, Float> privateer, Map<String, Float> player,
                            String playerFactionId, String privateerFactionId) {
        for (Map.Entry<String, Float> e : player.entrySet()) {
            String id = e.getKey();
            if (id.equals(playerFactionId) || id.equals(privateerFactionId)) continue;
            privateer.put(id, e.getValue());
        }
        privateer.put(playerFactionId, 1.0f);
        privateer.remove(privateerFactionId);
    }

    private PrivateerRelations() {}
}
