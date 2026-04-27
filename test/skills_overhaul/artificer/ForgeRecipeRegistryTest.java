package skills_overhaul.artificer;

import skills_overhaul.artificer.forge.ForgeDeviceRecipe;
import skills_overhaul.artificer.forge.ForgeDeviceType;
import skills_overhaul.artificer.forge.ForgeRecipeRegistry;

public class ForgeRecipeRegistryTest {
    public static void main(String[] args) throws Exception {
        ForgeRecipeRegistry.clearForTest();

        ForgeDeviceRecipe r1 = new ForgeDeviceRecipe(
                "drone_wakes", "Drone Wakes", ForgeDeviceType.BATTLE,
                5f, 0, "drone_wakes_penalty", "drone_wakes", "spawns PD drones");
        ForgeRecipeRegistry.register(r1);

        assert ForgeRecipeRegistry.get("drone_wakes") == r1;
        assert !ForgeRecipeRegistry.isUnlocked("drone_wakes");
        assert ForgeRecipeRegistry.getAllUnlocked().isEmpty();

        ForgeRecipeRegistry.unlock("drone_wakes");
        assert ForgeRecipeRegistry.isUnlocked("drone_wakes");
        assert ForgeRecipeRegistry.getAllUnlocked().contains(r1);
        assert ForgeRecipeRegistry.getAllUnlocked(ForgeDeviceType.BATTLE).contains(r1);
        assert ForgeRecipeRegistry.getAllUnlocked(ForgeDeviceType.CAMPAIGN).isEmpty();

        ForgeRecipeRegistry.lock("drone_wakes");
        assert !ForgeRecipeRegistry.isUnlocked("drone_wakes");

        System.out.println("ALL PASSED");
    }
}
