package skills_overhaul.pilot.gunnery;

public final class KillingMachineState {
    private static final int MAX_STACKS = 5;
    private int stacks = 0;

    public void onKill() { stacks = Math.min(MAX_STACKS, stacks + 1); }
    public int getStacks() { return stacks; }
    public float getDamageBonus() { return stacks * 0.03f; }
    public float getGlassCannonPenaltyRelief() { return stacks * 0.02f; }
    public void reset() { stacks = 0; }
}
