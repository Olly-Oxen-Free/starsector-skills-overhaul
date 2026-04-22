package skills_overhaul.xp;

public class DamageLedgerTest {
    public static void main(String[] args) {
        DamageLedger led = new DamageLedger();
        led.addDirect("enemy_a", 40f, false);
        led.addDirect("enemy_a", 10f, true);
        led.addFighter("enemy_a", 20f, false);
        led.addTotal("enemy_a", 100f);

        DamageLedger.Entry e = led.getEntry("enemy_a");
        assert Math.abs(e.directManual - 40f) < 1e-3 : "dm";
        assert Math.abs(e.directAuto - 10f)   < 1e-3 : "da";
        assert Math.abs(e.fighterManual - 20f)< 1e-3 : "fm";
        assert Math.abs(e.fighterAuto - 0f)   < 1e-3 : "fa";
        assert Math.abs(e.totalToShip - 100f) < 1e-3 : "tot";

        // eff_direct = 0.80*10 + 1.00*40 = 48; share = 48/100 = 0.48
        assert Math.abs(led.effectiveDirectShare("enemy_a", 0.80f) - 0.48f) < 1e-3 : "edshare";
        // eff_fighter = 0.80*0 + 1.00*20 = 20; share = 20/100 = 0.20
        assert Math.abs(led.effectiveFighterShare("enemy_a", 0.80f) - 0.20f) < 1e-3 : "efshare";

        // Missing key returns 0
        assert led.effectiveDirectShare("missing", 0.80f) == 0f : "missing key = 0";

        System.out.println("ALL PASSED");
    }
}
