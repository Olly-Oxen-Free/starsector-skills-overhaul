package skills_overhaul.tree;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Boolean prereq clause model. A node's prereqs are an AND-joined list of clauses.
 * Each clause is one of:
 *   {"all":  [ids...]}                 — every id owned
 *   {"any":  [ids...]}                 — at least one owned
 *   {"atLeast": N, "of": [ids...]}     — N-of-set owned
 */
public final class Prereq {

    public interface Clause {
        boolean satisfied(Set<String> ownedIds);
        Set<String> referencedIds();
    }

    public static final class All implements Clause {
        private final List<String> ids;
        public All(List<String> ids) { this.ids = Collections.unmodifiableList(ids); }
        public boolean satisfied(Set<String> owned) {
            for (String id : ids) if (!owned.contains(id)) return false;
            return true;
        }
        public Set<String> referencedIds() { return new LinkedHashSet<>(ids); }
        public List<String> getIds() { return ids; }
    }

    public static final class Any implements Clause {
        private final List<String> ids;
        public Any(List<String> ids) { this.ids = Collections.unmodifiableList(ids); }
        public boolean satisfied(Set<String> owned) {
            for (String id : ids) if (owned.contains(id)) return true;
            return false;
        }
        public Set<String> referencedIds() { return new LinkedHashSet<>(ids); }
        public List<String> getIds() { return ids; }
    }

    public static final class AtLeast implements Clause {
        private final int n;
        private final List<String> ids;
        public AtLeast(int n, List<String> ids) {
            this.n = n;
            this.ids = Collections.unmodifiableList(ids);
        }
        public boolean satisfied(Set<String> owned) {
            int c = 0;
            for (String id : ids) if (owned.contains(id)) { c++; if (c >= n) return true; }
            return c >= n;
        }
        public Set<String> referencedIds() { return new LinkedHashSet<>(ids); }
        public int getN() { return n; }
        public List<String> getIds() { return ids; }
    }

    public static Clause parseClause(JSONObject o) throws JSONException {
        if (o.has("all")) {
            return new All(toStringList(o.getJSONArray("all")));
        }
        if (o.has("any")) {
            return new Any(toStringList(o.getJSONArray("any")));
        }
        if (o.has("atLeast")) {
            if (!o.has("of")) {
                throw new JSONException("atLeast clause missing 'of' array: " + o);
            }
            return new AtLeast(o.getInt("atLeast"), toStringList(o.getJSONArray("of")));
        }
        throw new JSONException("unknown prereq clause type: " + o);
    }

    public static List<Clause> parseList(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) return Collections.emptyList();
        List<Clause> out = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) out.add(parseClause(arr.getJSONObject(i)));
        return Collections.unmodifiableList(out);
    }

    public static boolean allSatisfied(List<Clause> clauses, Set<String> owned) {
        for (Clause c : clauses) if (!c.satisfied(owned)) return false;
        return true;
    }

    public static Set<String> allReferencedIds(List<Clause> clauses) {
        Set<String> out = new LinkedHashSet<>();
        for (Clause c : clauses) out.addAll(c.referencedIds());
        return out;
    }

    private static List<String> toStringList(JSONArray a) throws JSONException {
        List<String> out = new ArrayList<>(a.length());
        for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        return out;
    }

    private Prereq() {}
}
