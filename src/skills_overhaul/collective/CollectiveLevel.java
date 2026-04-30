package skills_overhaul.collective;

public final class CollectiveLevel {
    private static final int MAX_LEVEL = 3;

    public static int fromAlphaCount(int alphaCount) { return Math.min(alphaCount, MAX_LEVEL); }
    public static int getInterfaceUses(int level) { return level; }

    private CollectiveLevel() {}
}
