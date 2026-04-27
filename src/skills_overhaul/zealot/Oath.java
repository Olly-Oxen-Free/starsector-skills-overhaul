package skills_overhaul.zealot;

public enum Oath {
    BLACK_FLAG,
    DEFENDER,
    MANDATE,
    SINGULARITY;

    public static Oath fromKey(String key) {
        if (key == null) return null;
        for (Oath o : values()) if (o.name().equals(key)) return o;
        return null;
    }
}
