package skills_overhaul.collective;

/** Pure rule for which fighters Wide-Range Propagation buffs (owner 0 = player side). */
public final class WideRangePropagationRule {

    public static boolean shouldBuff(int fighterOwner, int sourceOwner, boolean sourceAICored) {
        return fighterOwner == 0 && sourceOwner == 0 && sourceAICored;
    }

    private WideRangePropagationRule() {}
}
