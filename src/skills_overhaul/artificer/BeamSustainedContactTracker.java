package skills_overhaul.artificer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BeamSustainedContactTracker {

    public static final float THRESHOLD_SECONDS = 3f;
    public static final float DISSIPATION_DEBUFF_MULT = 0.85f;

    /** key = attacker, value = (target → contact seconds). */
    private final Map<Object, Map<Object, Float>> CONTACT = new HashMap<>();

    public void onBeamHit(Object attacker, Object target, float deltaTime) {
        Map<Object, Float> perAttacker = CONTACT.get(attacker);
        if (perAttacker == null) {
            perAttacker = new HashMap<>();
            CONTACT.put(attacker, perAttacker);
        }
        Float cur = perAttacker.get(target);
        if (cur == null) cur = 0f;
        perAttacker.put(target, cur + deltaTime);
    }

    public void onBeamMissed(Object attacker, Object target) {
        Map<Object, Float> perAttacker = CONTACT.get(attacker);
        if (perAttacker != null) perAttacker.remove(target);
    }

    public boolean hasDebuff(Object target) {
        for (Map<Object, Float> perAttacker : CONTACT.values()) {
            Float t = perAttacker.get(target);
            if (t != null && t >= THRESHOLD_SECONDS) return true;
        }
        return false;
    }

    public float getDebuffMult(Object target) {
        return hasDebuff(target) ? DISSIPATION_DEBUFF_MULT : 1f;
    }

    /** Snapshot of targets currently tracked for the given attacker (used to reconcile lost contact). */
    public Set<Object> getTargets(Object attacker) {
        Map<Object, Float> perAttacker = CONTACT.get(attacker);
        if (perAttacker == null) return Collections.emptySet();
        return new HashSet<Object>(perAttacker.keySet());
    }

    public void clear() { CONTACT.clear(); }
}
