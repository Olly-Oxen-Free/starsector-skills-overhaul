package skills_overhaul.artificer;

import skills_overhaul.artificer.forge.ForgeService;

public class ForgeServiceTest {
    public static void main(String[] args) throws Exception {
        long now = 1_000_000_000L;
        long cdEnd = now + 86_400_000L * 3;

        assert ForgeService.cooldownActive(cdEnd, now);
        assert ForgeService.cooldownActive(cdEnd, now + 86_400_000L * 2);
        assert !ForgeService.cooldownActive(cdEnd, now + 86_400_000L * 3);
        assert !ForgeService.cooldownActive(cdEnd, now + 86_400_000L * 4);

        assert !ForgeService.cooldownActive(0L, now);

        System.out.println("ALL PASSED");
    }
}
