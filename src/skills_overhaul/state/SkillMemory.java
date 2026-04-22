package skills_overhaul.state;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.util.Map;

public final class SkillMemory {
    public static String xpKey(Tree t)       { return "$skills." + t.key + "_xp"; }
    public static String levelKey(Tree t)    { return "$skills." + t.key + "_level"; }
    public static String pointsKey(Tree t)   { return "$skills.points." + t.key; }
    public static String freeUniKey()        { return "$skills.points.universal_free"; }
    public static String migUniKey(Tree t)   { return "$skills.points.universal_migrated_" + t.key; }
    public static String purchasedKey(String nodeId) { return "$skills.purchased." + nodeId; }
    public static String respecUsedKey(Tree t) { return "$skills.respec_used." + t.key; }
    public static final String MIGRATED_KEY = "$skills_overhaul.migrated";

    // Testable Map methods
    public static float getXP(Map<String,Object> m, Tree t) {
        Object v = m.get(xpKey(t)); return v == null ? 0f : ((Number) v).floatValue();
    }
    public static void addXP(Map<String,Object> m, Tree t, float x) {
        m.put(xpKey(t), getXP(m, t) + x);
    }
    public static int getLevel(Map<String,Object> m, Tree t) {
        Object v = m.get(levelKey(t)); return v == null ? 0 : ((Number) v).intValue();
    }
    public static void setLevel(Map<String,Object> m, Tree t, int lv) { m.put(levelKey(t), lv); }

    public static int getPoints(Map<String,Object> m, Tree t) {
        Object v = m.get(pointsKey(t)); return v == null ? 0 : ((Number) v).intValue();
    }
    public static void addPoints(Map<String,Object> m, Tree t, int n) {
        m.put(pointsKey(t), getPoints(m, t) + n);
    }
    public static void setPoints(Map<String,Object> m, Tree t, int n) { m.put(pointsKey(t), n); }

    public static boolean isMigrated(Map<String,Object> m) {
        Object v = m.get(MIGRATED_KEY); return v != null && (Boolean) v;
    }
    public static void setMigrated(Map<String,Object> m, boolean v) { m.put(MIGRATED_KEY, v); }

    // Game-API wrappers
    public static float getXP(MemoryAPI mem, Tree t)          { return mem.getFloat(xpKey(t)); }
    public static void  addXP(MemoryAPI mem, Tree t, float x) { mem.set(xpKey(t), getXP(mem, t) + x); }
    public static int   getLevel(MemoryAPI mem, Tree t)       { return (int) mem.getFloat(levelKey(t)); }
    public static void  setLevel(MemoryAPI mem, Tree t, int lv) { mem.set(levelKey(t), (float) lv); }
    public static int   getPoints(MemoryAPI mem, Tree t)      { return (int) mem.getFloat(pointsKey(t)); }
    public static void  addPoints(MemoryAPI mem, Tree t, int n) { mem.set(pointsKey(t), (float)(getPoints(mem,t)+n)); }
    public static boolean isMigrated(MemoryAPI mem)           { return mem.getBoolean(MIGRATED_KEY); }
    public static void  setMigrated(MemoryAPI mem, boolean v) { mem.set(MIGRATED_KEY, v); }
}
