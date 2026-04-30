package skills_overhaul.collective;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * Production adapter: wraps MemoryAPI to implement MemoryView.
 * Memory entries set with TTL=0 (persistent until explicitly unset).
 */
final class MemoryAPIView implements MemoryView {
    private final MemoryAPI mem;

    MemoryAPIView(MemoryAPI mem) {
        this.mem = mem;
    }

    @Override
    public void set(String key, Object value) {
        mem.set(key, value, 0f);
    }

    @Override
    public Object get(String key) {
        return mem.get(key);
    }

    @Override
    public void unset(String key) {
        mem.unset(key);
    }
}
