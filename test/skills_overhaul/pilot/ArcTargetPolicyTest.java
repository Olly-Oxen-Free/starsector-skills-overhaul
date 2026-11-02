package skills_overhaul.pilot;

public class ArcTargetPolicyTest {
    public static void main(String[] args) {
        Object flagship = new Object();
        Object enemy = new Object();

        assert ArcTargetPolicy.dischargeTarget(flagship, enemy) == enemy : "arc goes to the enemy";
        assert ArcTargetPolicy.dischargeTarget(flagship, null) == null : "no enemy in range -> no arc";
        assert ArcTargetPolicy.dischargeTarget(flagship, flagship) == null : "never arc into the source";

        System.out.println("ALL PASSED");
    }
}
