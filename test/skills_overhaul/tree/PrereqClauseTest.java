package skills_overhaul.tree;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class PrereqClauseTest {
    public static void main(String[] args) throws Exception {
        Set<String> owned = new HashSet<>();

        // --- All ---
        Prereq.Clause all = Prereq.parseClause(new JSONObject("{\"all\":[\"a\",\"b\"]}"));
        assert !all.satisfied(owned) : "All: empty owned -> false";
        owned.add("a");
        assert !all.satisfied(owned) : "All: partial owned -> false";
        owned.add("b");
        assert all.satisfied(owned) : "All: complete owned -> true";

        // --- Any ---
        Set<String> owned2 = new HashSet<>();
        Prereq.Clause any = Prereq.parseClause(new JSONObject("{\"any\":[\"x\",\"y\"]}"));
        assert !any.satisfied(owned2) : "Any: empty -> false";
        owned2.add("x");
        assert any.satisfied(owned2) : "Any: one -> true";

        // --- AtLeast ---
        Set<String> owned3 = new HashSet<>();
        Prereq.Clause al = Prereq.parseClause(
                new JSONObject("{\"atLeast\":2,\"of\":[\"p\",\"q\",\"r\"]}"));
        assert !al.satisfied(owned3) : "AtLeast 2: 0 -> false";
        owned3.add("p");
        assert !al.satisfied(owned3) : "AtLeast 2: 1 -> false";
        owned3.add("q");
        assert al.satisfied(owned3) : "AtLeast 2: 2 -> true";
        owned3.add("r");
        assert al.satisfied(owned3) : "AtLeast 2: 3 -> true";

        // --- referencedIds ---
        Set<String> refs = al.referencedIds();
        assert refs.contains("p") && refs.contains("q") && refs.contains("r") && refs.size() == 3
                : "referencedIds returns the full set";

        // --- parseList AND-joined ---
        JSONArray arr = new JSONArray("[{\"all\":[\"a\"]},{\"any\":[\"b\",\"c\"]}]");
        List<Prereq.Clause> clauses = Prereq.parseList(arr);
        assert clauses.size() == 2;
        Set<String> o4 = new HashSet<>();
        o4.add("a");
        assert !Prereq.allSatisfied(clauses, o4) : "AND: only first clause sat -> false";
        o4.add("c");
        assert Prereq.allSatisfied(clauses, o4) : "AND: both clauses sat -> true";

        // --- parseList null/empty -> empty list (root nodes) ---
        assert Prereq.parseList(null).isEmpty();
        assert Prereq.parseList(new JSONArray()).isEmpty();
        assert Prereq.allSatisfied(Prereq.parseList(null), new HashSet<String>())
                : "empty clause list always satisfied";

        // --- malformed clause throws ---
        boolean threw = false;
        try {
            Prereq.parseClause(new JSONObject("{\"unknown\":[\"a\"]}"));
        } catch (Exception e) { threw = true; }
        assert threw : "unknown clause type rejected";

        threw = false;
        try {
            Prereq.parseClause(new JSONObject("{\"atLeast\":2}"));
        } catch (Exception e) { threw = true; }
        assert threw : "atLeast without 'of' rejected";

        System.out.println("ALL PASSED");
    }
}
