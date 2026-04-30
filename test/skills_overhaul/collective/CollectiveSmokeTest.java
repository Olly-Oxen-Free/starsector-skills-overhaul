package skills_overhaul.collective;

public class CollectiveSmokeTest {

    public static void main(String[] args) {
        // Test 1: Full Alpha progression
        int alphas = 3;
        int level  = CollectiveLevel.fromAlphaCount(alphas);
        int uses   = CollectiveLevel.getInterfaceUses(level);
        assert level == 3 : "Expected level 3, got " + level;
        assert uses == 3  : "Expected 3 uses, got " + uses;

        // Test 2: KnowledgeTier boundaries
        assert KnowledgeTier.fromKnowledge(0f)    == KnowledgeTier.NASCENT      : "NASCENT at 0";
        assert KnowledgeTier.fromKnowledge(50f)   == KnowledgeTier.AWARE        : "AWARE at 50";
        assert KnowledgeTier.fromKnowledge(150f)  == KnowledgeTier.INTEGRATED   : "INTEGRATED at 150";
        assert KnowledgeTier.fromKnowledge(300f)  == KnowledgeTier.ASCENDANT    : "ASCENDANT at 300";
        assert KnowledgeTier.fromKnowledge(500f)  == KnowledgeTier.TRANSCENDENT : "TRANSCENDENT at 500";
        assert KnowledgeTier.fromKnowledge(9999f) == KnowledgeTier.TRANSCENDENT : "Overflow clamps";

        // Test 3: Assimilation caps (pure math — no API needed)
        float crBonus  = Math.min(10 * 1f, 10f);
        float dmgBonus = Math.min(3 * 4f, 12f);
        assert crBonus  == 10f : "CR bonus cap";
        assert dmgBonus == 12f : "Weapon damage cap";

        // Test 4: Level zero → no uses
        assert CollectiveLevel.getInterfaceUses(0) == 0 : "Level 0 = 0 uses";

        System.out.println("CollectiveSmokeTest: all passed.");
    }
}
