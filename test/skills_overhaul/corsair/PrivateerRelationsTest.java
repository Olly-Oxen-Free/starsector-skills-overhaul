package skills_overhaul.corsair;

import java.util.HashMap;
import java.util.Map;

public class PrivateerRelationsTest {
    public static void main(String[] args) {
        // The privateer faction was set hostile to Tri-Tachyon by an earlier commission.
        Map<String, Float> priv = new HashMap<>();
        priv.put("tritachyon", -1f);
        priv.put("player", 1f);

        // The player has since made peace with Tri-Tachyon and gone to war with the Hegemony.
        Map<String, Float> player = new HashMap<>();
        player.put("tritachyon", 0f);
        player.put("hegemony", -0.8f);
        player.put("pirates", -1f);

        PrivateerRelations.sync(priv, player, "player", "skills_overhaul_privateer");

        // test 1: a hostility the player has ended is undone
        assert priv.get("tritachyon") == 0f : "stale -1 towards a faction now at peace must be reset";

        // test 2: a hostility that started after the commission is picked up, at the player's value
        assert priv.get("hegemony") == -0.8f;
        assert priv.get("pirates") == -1f;

        // test 3: the privateer stays friendly to the player and is never related to itself
        assert priv.get("player") == 1f;
        player.put("skills_overhaul_privateer", 1f);
        PrivateerRelations.sync(priv, player, "player", "skills_overhaul_privateer");
        assert !priv.containsKey("skills_overhaul_privateer");
        assert priv.get("player") == 1f;

        System.out.println("PrivateerRelationsTest: all passed.");
    }
}
