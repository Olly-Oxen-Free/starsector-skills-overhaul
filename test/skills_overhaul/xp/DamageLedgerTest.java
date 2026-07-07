package skills_overhaul.xp;

public class DamageLedgerTest {
    public static void main(String[] args) {
        DamageLedger led = new DamageLedger();
        led.addDirect("enemy_a", 40f, false);
        led.addDirect("enemy_a", 10f, true);
        led.addFighter("enemy_a", 20f, false);
        led.addTotal("enemy_a", 100f);

        // Recorded amounts verified through the public share API (mult=1 → raw sums):
        // direct = 40 + 10 = 50/100; fighter = 20/100
        assert Math.abs(led.effectiveDirectShare("enemy_a", 1f) - 0.50f) < 1e-3 : "raw direct";
        assert Math.abs(led.effectiveFighterShare("enemy_a", 1f) - 0.20f) < 1e-3 : "raw fighter";

        // eff_direct = 0.80*10 + 1.00*40 = 48; share = 48/100 = 0.48
        assert Math.abs(led.effectiveDirectShare("enemy_a", 0.80f) - 0.48f) < 1e-3 : "edshare";
        // eff_fighter = 0.80*0 + 1.00*20 = 20; share = 20/100 = 0.20
        assert Math.abs(led.effectiveFighterShare("enemy_a", 0.80f) - 0.20f) < 1e-3 : "efshare";

        // Missing key returns 0
        assert led.effectiveDirectShare("missing", 0.80f) == 0f : "missing key = 0";

        System.out.println("ALL PASSED");
    }
}
