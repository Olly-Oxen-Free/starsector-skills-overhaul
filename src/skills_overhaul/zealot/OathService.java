package skills_overhaul.zealot;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.Map;

public final class OathService {

    public static final int SWAP_SP_COST = 3;
    public static final String K_OATH = "$skills.zealot.oath";

    public static Oath getCurrentOath(Map<String,Object> mem) {
        Object v = mem.get(K_OATH);
        return v instanceof String ? Oath.fromKey((String) v) : null;
    }

    public static void setOath(Map<String,Object> mem, Oath oath) {
        if (oath == null) mem.remove(K_OATH);
        else mem.put(K_OATH, oath.name());
    }

    public static boolean allowsAICoreOfficer(Map<String,Object> mem) {
        return getCurrentOath(mem) != Oath.MANDATE;
    }

    public static Oath getCurrentOath(MemoryAPI mem) {
        Object v = mem.get(K_OATH);
        return v instanceof String ? Oath.fromKey((String) v) : null;
    }

    public static void setOath(MemoryAPI mem, Oath oath) {
        if (oath == null) mem.unset(K_OATH);
        else mem.set(K_OATH, oath.name());
    }

    public static boolean allowsAICoreOfficer(MemoryAPI mem) {
        return getCurrentOath(mem) != Oath.MANDATE;
    }

    private OathService() {}
}
