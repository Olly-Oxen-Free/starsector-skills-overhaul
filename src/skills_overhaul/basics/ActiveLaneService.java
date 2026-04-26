package skills_overhaul.basics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import skills_overhaul.state.SkillMemory;

import java.util.*;

/**
 * Active Basics: only 2 of N purchased T0 nodes apply their `core` channel effects.
 * State lives in memory under `$skills.activeBasics` as a List<String> of node ids.
 *
 * Pure logic — game-API checks (combat, current sector) are passed in by callers.
 */
public final class ActiveLaneService {

    public static final int MAX_ACTIVE = 2;

    public static List<String> getActive(Map<String,Object> mem) {
        return new ArrayList<>(SkillMemory.getActiveBasics(mem));
    }

    public static boolean isActive(Map<String,Object> mem, String nodeId) {
        return SkillMemory.getActiveBasics(mem).contains(nodeId);
    }

    /** Out-of-combat (live game): {@code !inCombat}. Pure for testability. */
    public static boolean canSwap(boolean inCombat) {
        return !inCombat;
    }

    /** Live convenience: queries the running combat engine. */
    public static boolean canSwapLive() {
        return canSwap(Global.getCombatEngine() != null);
    }

    /** Auto-add on purchase if there's an empty slot. */
    public static void onPurchased(Map<String,Object> mem, String nodeId) {
        List<String> cur = SkillMemory.getActiveBasics(mem);
        if (cur.contains(nodeId)) return;
        if (cur.size() < MAX_ACTIVE) {
            cur.add(nodeId);
            SkillMemory.setActiveBasics(mem, cur);
        }
    }

    /** Drop from active set on respec/refund. */
    public static void onUnpurchased(Map<String,Object> mem, String nodeId) {
        List<String> cur = SkillMemory.getActiveBasics(mem);
        if (cur.remove(nodeId)) SkillMemory.setActiveBasics(mem, cur);
    }

    /**
     * Swap newId into oldId's slot. Returns false if oldId isn't active or newId already is.
     * Caller is responsible for the in-combat check (use canSwapLive() or canSwap(boolean)).
     */
    public static boolean swap(Map<String,Object> mem, String oldId, String newId) {
        List<String> cur = SkillMemory.getActiveBasics(mem);
        int i = cur.indexOf(oldId);
        if (i < 0) return false;
        if (cur.contains(newId)) return false;
        cur.set(i, newId);
        SkillMemory.setActiveBasics(mem, cur);
        return true;
    }

    /** Add to first empty slot. Returns false if full or already present. */
    public static boolean add(Map<String,Object> mem, String nodeId) {
        List<String> cur = SkillMemory.getActiveBasics(mem);
        if (cur.contains(nodeId)) return false;
        if (cur.size() >= MAX_ACTIVE) return false;
        cur.add(nodeId);
        SkillMemory.setActiveBasics(mem, cur);
        return true;
    }

    // ---- MemoryAPI overloads ----

    @SuppressWarnings("unchecked")
    public static boolean isActive(MemoryAPI mem, String nodeId) {
        Object v = mem.get(SkillMemory.activeBasicsKey());
        return (v instanceof List) && ((List<String>) v).contains(nodeId);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getActive(MemoryAPI mem) {
        Object v = mem.get(SkillMemory.activeBasicsKey());
        if (v instanceof List) return new ArrayList<>((List<String>) v);
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static void onPurchased(MemoryAPI mem, String nodeId) {
        List<String> cur = readList(mem);
        if (cur.contains(nodeId)) return;
        if (cur.size() < MAX_ACTIVE) {
            cur.add(nodeId);
            mem.set(SkillMemory.activeBasicsKey(), cur);
        }
    }

    @SuppressWarnings("unchecked")
    public static void onUnpurchased(MemoryAPI mem, String nodeId) {
        List<String> cur = readList(mem);
        if (cur.remove(nodeId)) {
            mem.set(SkillMemory.activeBasicsKey(), cur);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readList(MemoryAPI mem) {
        Object v = mem.get(SkillMemory.activeBasicsKey());
        if (v instanceof List) return new ArrayList<>((List<String>) v);
        return new ArrayList<>();
    }

    private ActiveLaneService() {}
}
