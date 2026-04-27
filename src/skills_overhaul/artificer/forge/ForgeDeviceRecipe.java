package skills_overhaul.artificer.forge;

public final class ForgeDeviceRecipe {
    public final String id;
    public final String displayName;
    public final ForgeDeviceType type;
    public final float supplyCost;
    public final int cooldownDays;
    public final String penaltyKey;
    public final String effectHandlerKey;
    public final String descriptionShort;

    public ForgeDeviceRecipe(String id, String displayName, ForgeDeviceType type,
                             float supplyCost, int cooldownDays,
                             String penaltyKey, String effectHandlerKey,
                             String descriptionShort) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.supplyCost = supplyCost;
        this.cooldownDays = cooldownDays;
        this.penaltyKey = penaltyKey;
        this.effectHandlerKey = effectHandlerKey;
        this.descriptionShort = descriptionShort;
    }
}
