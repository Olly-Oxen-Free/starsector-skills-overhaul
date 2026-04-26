package skills_overhaul.tree;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public final class Node {
    private final String id, name, desc, icon;
    private final int x, y, cost;
    private final List<Prereq.Clause> clauses;
    private final List<JSONObject> effects;

    public Node(String id, String name, String desc, int x, int y, int cost,
                List<Prereq.Clause> clauses, List<JSONObject> effects, String icon) {
        this.id = id; this.name = name; this.desc = desc;
        this.x = x; this.y = y; this.cost = cost;
        this.clauses = clauses;
        this.effects = effects;
        this.icon = icon;
    }

    public String getId()                  { return id; }
    public String getName()                { return name; }
    public String getDesc()                { return desc; }
    public String getIcon()                { return icon; }
    public int    getX()                   { return x; }
    public int    getY()                   { return y; }
    public int    getCost()                { return cost; }
    public List<Prereq.Clause> getClauses(){ return clauses; }
    public List<JSONObject> getEffects()   { return effects; }

    /** Flat set of every node id referenced by this node's prereq clauses. */
    public Set<String> getReferencedPrereqIds() {
        return Prereq.allReferencedIds(clauses);
    }

    // -------- Backward-compat accessors (kept so existing UI / GameSpend compile) --------
    public int    getTier()                { return y; }
    public int    getSlot()                { return x; }
    public String getLane()                { return null; }
    public boolean isCapstone()            { return y == 5; }
    public boolean isMerge()               { return false; }
    public List<String> getParentLanes()   { return Collections.emptyList(); }

    /**
     * Legacy: returns the flat list of referenced ids. Equivalent semantically only
     * for old-style "all" prereqs; preserved so PrereqValidator's existing call sites
     * continue to compile until they migrate to clause-aware methods.
     */
    public List<String> getPrereqs() {
        return new ArrayList<>(getReferencedPrereqIds());
    }
    // ---------------------------------------------------------------------------

    public static Node fromJSON(JSONObject o) throws JSONException {
        List<JSONObject> fx = new ArrayList<>();
        JSONArray fa = o.optJSONArray("effects");
        if (fa != null) for (int i = 0; i < fa.length(); i++) fx.add(fa.getJSONObject(i));

        return new Node(
                o.getString("id"),
                o.getString("name"),
                o.optString("desc", ""),
                o.getInt("x"),
                o.getInt("y"),
                o.optInt("cost", 1),
                Prereq.parseList(o.optJSONArray("prereqs")),
                Collections.unmodifiableList(fx),
                o.optString("icon", null));
    }
}
