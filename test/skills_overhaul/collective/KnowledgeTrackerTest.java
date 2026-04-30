package skills_overhaul.collective;

public class KnowledgeTrackerTest {

    static class MockMemory implements MemoryView {
        private final java.util.HashMap<String, Object> data = new java.util.HashMap<>();
        public void set(String k, Object v) { data.put(k, v); }
        public Object get(String k) { return data.get(k); }
        public void unset(String k) { data.remove(k); }
    }

    public static void main(String[] args) {
        MockMemory mem = new MockMemory();
        String coreId = "alpha_001";

        assert KnowledgeTracker.getKnowledge(mem, coreId) == 0f;
        assert KnowledgeTracker.getTier(mem, coreId) == KnowledgeTier.NASCENT;

        KnowledgeTracker.addKnowledge(mem, coreId, 60f);
        assert KnowledgeTracker.getKnowledge(mem, coreId) == 60f;
        assert KnowledgeTracker.getTier(mem, coreId) == KnowledgeTier.AWARE;

        KnowledgeTracker.addKnowledge(mem, coreId, 200f);
        assert KnowledgeTracker.getTier(mem, coreId) == KnowledgeTier.INTEGRATED;

        KnowledgeTracker.reset(mem, coreId);
        assert KnowledgeTracker.getKnowledge(mem, coreId) == 0f;
        assert KnowledgeTracker.getTier(mem, coreId) == KnowledgeTier.NASCENT;

        System.out.println("KnowledgeTrackerTest PASSED");
    }
}
