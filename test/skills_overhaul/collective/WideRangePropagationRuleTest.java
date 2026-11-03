package skills_overhaul.collective;

public class WideRangePropagationRuleTest {
    public static void main(String[] args) {
        // test 1: player fighter from a player AI-cored carrier is buffed
        assert WideRangePropagationRule.shouldBuff(0, 0, true);

        // test 2: enemy fighter from an enemy AI-cored carrier (Remnant) is not buffed
        assert !WideRangePropagationRule.shouldBuff(1, 1, true)
                : "enemy AI-cored carrier's fighter must not get the player's buff";

        // test 3: either side not the player's is not buffed
        assert !WideRangePropagationRule.shouldBuff(1, 0, true);
        assert !WideRangePropagationRule.shouldBuff(0, 1, true);

        // test 4: player carrier without an AI core is not buffed
        assert !WideRangePropagationRule.shouldBuff(0, 0, false);

        System.out.println("WideRangePropagationRuleTest: all passed.");
    }
}
