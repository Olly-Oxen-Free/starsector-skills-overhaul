package skills_overhaul.state;

public enum Tree {
    PILOT("pilot"),
    COMMANDER("commander"),
    ADMIN("admin");

    public final String key;
    Tree(String k) { this.key = k; }

    /** Prefix of {@code Node#getId()} values in JSON (e.g. {@code so_p_g0}). */
    public String nodeIdPrefix() {
        switch (this) {
            case PILOT:     return "so_p_";
            case COMMANDER: return "so_c_";
            case ADMIN:     return "so_a_";
            default:        return "so_";
        }
    }
}
