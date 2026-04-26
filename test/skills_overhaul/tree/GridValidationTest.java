package skills_overhaul.tree;

import org.json.JSONObject;

public class GridValidationTest {
    public static void main(String[] args) throws Exception {
        // --- minimal valid tree (T0 only) ---
        String ok = "{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}" +
                "]}";
        TreeDef def = TreeLoader.fromJSON(new JSONObject(ok));
        assert def.getT0() != null && def.getT0().getId().equals("a");

        // --- T0 not at x=2 -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":1,\"y\":0,\"cost\":1,\"prereqs\":[]}" +
                "]}", "T0 must be at x=2");

        // --- T5 not at x=2 -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"z\",\"name\":\"Z\",\"x\":0,\"y\":5,\"cost\":1,\"prereqs\":[{\"all\":[\"a\"]}]}" +
                "]}", "T5 must be at x=2");

        // --- duplicate (x,y) -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"b\",\"name\":\"B\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}" +
                "]}", "duplicate coord");

        // --- x out of range -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":5,\"y\":1,\"cost\":1,\"prereqs\":[]}" +
                "]}", "x out of range");

        // --- y out of range -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":2,\"y\":6,\"cost\":1,\"prereqs\":[]}" +
                "]}", "y out of range");

        // --- T0 with non-empty prereqs -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":2,\"y\":0,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"x\"]}]}" +
                "]}", "T0 must have empty prereqs");

        // --- prereq references unknown id -> reject ---
        expectFail("{\"tree\":\"t\",\"nodes\":[" +
                "{\"id\":\"a\",\"name\":\"A\",\"x\":2,\"y\":0,\"cost\":1,\"prereqs\":[]}," +
                "{\"id\":\"b\",\"name\":\"B\",\"x\":1,\"y\":1,\"cost\":1," +
                " \"prereqs\":[{\"all\":[\"ghost\"]}]}" +
                "]}", "unknown prereq id");

        System.out.println("ALL PASSED");
    }

    private static void expectFail(String json, String why) {
        try {
            TreeLoader.fromJSON(new JSONObject(json));
            throw new AssertionError("expected reject: " + why);
        } catch (AssertionError ae) { throw ae; }
        catch (Exception e) { /* expected */ }
    }
}
