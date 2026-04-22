package skills_overhaul.state;

public enum Tree {
    PILOT("pilot"), COMMANDER("commander"), ADMIN("admin");
    public final String key;
    Tree(String k) { this.key = k; }
}
