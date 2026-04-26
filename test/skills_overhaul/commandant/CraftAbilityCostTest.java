package skills_overhaul.commandant;

public class CraftAbilityCostTest {
    public static void main(String[] args) throws Exception {
        assert CraftSuppliesAbility.COST == 10f;
        assert CraftSuppliesAbility.OUTPUT == 10f;
        assert CraftSuppliesAbility.COOLDOWN_DAYS == 2f;
        assert CraftFuelAbility.COST == 20f;
        assert CraftFuelAbility.OUTPUT == 1f;
        assert CraftFuelAbility.COOLDOWN_DAYS == 2f;
        System.out.println("ALL PASSED");
    }
}
