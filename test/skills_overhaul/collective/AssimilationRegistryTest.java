package skills_overhaul.collective;

public class AssimilationRegistryTest {

    static class MockMemory implements MemoryView {
        private final java.util.HashMap<String, Object> data = new java.util.HashMap<>();
        public void set(String k, Object v) { data.put(k, v); }
        public Object get(String k) { return data.get(k); }
        public void unset(String k) { data.remove(k); }
    }

    public static void main(String[] args) {
        MockMemory mem = new MockMemory();

        assert AssimilationRegistry.getGammaCount(mem) == 0;
        assert AssimilationRegistry.getBetaCount(mem) == 0;
        assert AssimilationRegistry.getAlphaCount(mem) == 0;
        assert AssimilationRegistry.getCRBonus(mem) == 0f;
        assert AssimilationRegistry.getFluxDissipationBonus(mem) == 0f;
        assert AssimilationRegistry.getWeaponDamageBonus(mem) == 0f;

        AssimilationRegistry.assimilateGamma(mem);
        AssimilationRegistry.assimilateGamma(mem);
        AssimilationRegistry.assimilateGamma(mem);
        assert AssimilationRegistry.getGammaCount(mem) == 3;
        assert Math.abs(AssimilationRegistry.getCRBonus(mem) - 3f) < 0.01f : "3 gamma = +3% CR";

        for (int i = 0; i < 8; i++) AssimilationRegistry.assimilateGamma(mem);
        assert Math.abs(AssimilationRegistry.getCRBonus(mem) - 10f) < 0.01f : "CR bonus caps at 10%";

        AssimilationRegistry.assimilateBeta(mem);
        AssimilationRegistry.assimilateBeta(mem);
        assert Math.abs(AssimilationRegistry.getFluxDissipationBonus(mem) - 4f) < 0.01f;

        AssimilationRegistry.assimilateAlpha(mem);
        assert Math.abs(AssimilationRegistry.getWeaponDamageBonus(mem) - 4f) < 0.01f;

        System.out.println("AssimilationRegistryTest PASSED");
    }
}
