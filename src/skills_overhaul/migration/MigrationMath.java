package skills_overhaul.migration;

public final class MigrationMath {
    private MigrationMath() {}

    public static int grantUniversal(int vanillaSpent, int vanillaUnspent, float mult, int cap) {
        int total = vanillaSpent + vanillaUnspent;
        int granted = (int) Math.floor(total * mult);
        return Math.min(cap, granted);
    }
}
