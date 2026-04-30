package skills_overhaul.collective;

/**
 * Minimal memory abstraction used by KnowledgeTracker and AssimilationRegistry.
 * Production code wraps MemoryAPI via MemoryAPIView; tests supply MockMemory.
 */
public interface MemoryView {
    void set(String key, Object value);
    Object get(String key);
    void unset(String key);
}
