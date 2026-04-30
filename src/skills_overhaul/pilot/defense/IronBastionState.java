package skills_overhaul.pilot.defense;

public final class IronBastionState {
    private static final float ACTIVE_DAMAGE_GRACE = 0.5f;
    private float timeSinceLastDamage = 999f;

    public void onHullOrArmorDamage() { timeSinceLastDamage = 0f; }
    public void advance(float dt) { timeSinceLastDamage += dt; }
    public boolean isTakingActiveDamage() { return timeSinceLastDamage < ACTIVE_DAMAGE_GRACE; }

    public float computeFighterKillHeal(float fighterMaxHP) { return fighterMaxHP * 0.05f; }
}
