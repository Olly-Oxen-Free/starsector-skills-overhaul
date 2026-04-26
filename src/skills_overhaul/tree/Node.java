package skills_overhaul.tree;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public final class Node {
    private final String id, name, desc, lane, icon;
    private final int tier, slot, cost;
    private final List<String> prereqs;
    private final List<String> parentLanes;
    private final boolean capstone, merge;
    private final List<JSONObject> effects;

    public Node(String id, String name, String desc, String lane, int tier, int slot, int cost,
                List<String> prereqs, List<String> parentLanes, boolean capstone, boolean merge,
                List<JSONObject> effects, String icon) {
        this.id = id; this.name = name; this.desc = desc;
        this.lane = lane; this.tier = tier; this.slot = slot; this.cost = cost;
        this.prereqs = prereqs;
        this.parentLanes = parentLanes;
        this.capstone = capstone; this.merge = merge;
        this.effects = effects;
        this.icon = icon;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDesc()        { return desc; }
    public String getLane()        { return lane; }
    public String getIcon()        { return icon; }
    public int    getTier()        { return tier; }
    public int    getSlot()        { return slot; }
    public int    getCost()        { return cost; }
    public List<String> getPrereqs() { return prereqs; }
    public List<String> getParentLanes() { return parentLanes; }
    public boolean isCapstone()    { return capstone; }
    public boolean isMerge()       { return merge; }
    public List<JSONObject> getEffects() { return effects; }

    public static Node fromJSON(JSONObject o) throws JSONException {
        List<String> pre = new ArrayList<>();
        JSONArray pa = o.optJSONArray("prereqs");
        if (pa != null) for (int i = 0; i < pa.length(); i++) pre.add(pa.getString(i));

        List<JSONObject> fx = new ArrayList<>();
        JSONArray fa = o.optJSONArray("effects");
        if (fa != null) for (int i = 0; i < fa.length(); i++) fx.add(fa.getJSONObject(i));

        return new Node(
                o.getString("id"),
                o.getString("name"),
                o.optString("desc", ""),
                o.getString("lane"),
                o.optInt("tier", 0),
                o.optInt("slot", 1),
                o.optInt("cost", 1),
                Collections.unmodifiableList(pre),
                Collections.emptyList(),
                o.optBoolean("capstone", false),
                o.optBoolean("merge", false),
                Collections.unmodifiableList(fx),
                o.optString("icon", null));
    }
}
