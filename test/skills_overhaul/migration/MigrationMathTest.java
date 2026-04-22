package skills_overhaul.migration;

public class MigrationMathTest {
    public static void main(String[] args) {
        // floor((spent + unspent) * 1.5), cap 15, elite excluded
        assert MigrationMath.grantUniversal(0, 0, 1.5f, 15) == 0 : "zero";
        assert MigrationMath.grantUniversal(3, 0, 1.5f, 15) == 4 : "spent only: floor(4.5)";
        assert MigrationMath.grantUniversal(5, 3, 1.5f, 15) == 12 : "floor(12.0)";
        assert MigrationMath.grantUniversal(20, 0, 1.5f, 15) == 15 : "cap";

        System.out.println("ALL PASSED");
    }
}
