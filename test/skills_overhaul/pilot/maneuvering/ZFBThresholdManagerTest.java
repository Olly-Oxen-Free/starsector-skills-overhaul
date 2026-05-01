package skills_overhaul.pilot.maneuvering;

public class ZFBThresholdManagerTest {
    public static void main(String[] args) {
        ZFBThresholdManager m = new ZFBThresholdManager();

        assert Math.abs(m.getThreshold() - 0.20f) < 1e-4f;
        assert m.isInZFB(0.19f);
        assert !m.isInZFB(0.21f);

        m.applyZeroFluxMastery();
        assert Math.abs(m.getThreshold() - 0.35f) < 1e-4f;
        assert m.isInZFB(0.34f);
        assert !m.isInZFB(0.36f);

        m.applyUntouchable();
        assert Math.abs(m.getThreshold() - 0.50f) < 1e-4f;
        assert m.isInZFB(0.49f);

        m.reset();
        m.applyZeroFluxMastery();
        assert !m.checkZFMSurge(0.40f);
        assert m.checkZFMSurge(0.34f) : "first crossing triggers surge";
        assert !m.checkZFMSurge(0.30f);
        assert !m.checkZFMSurge(0.40f);
        assert m.checkZFMSurge(0.34f) : "new crossing triggers";

        System.out.println("ALL PASSED");
    }
}
