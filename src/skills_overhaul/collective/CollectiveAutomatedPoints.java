package skills_overhaul.collective;

import skills_overhaul.artificer.AutomatedShipPointService;

/**
 * Collective automated-ship-point grants, routed through AutomatedShipPointService (the
 * cap AutomatedShipCapApplier reads). Callers re-run AutomatedShipCapApplier afterwards.
 */
public final class CollectiveAutomatedPoints {

    public static final String T0_KEY = "so_collective_t0";
    public static final int T0_POINTS = 60;
    public static final String BATTLE_CONDITIONING_KEY = "so_collective_battle_conditioning";
    public static final int BATTLE_CONDITIONING_POINTS = 20;

    public static void grantT0() { AutomatedShipPointService.registerBonus(T0_KEY, T0_POINTS); }
    public static void revokeT0() { AutomatedShipPointService.unregisterBonus(T0_KEY); }

    public static void grantBattleConditioning() {
        AutomatedShipPointService.registerBonus(BATTLE_CONDITIONING_KEY, BATTLE_CONDITIONING_POINTS);
    }
    public static void revokeBattleConditioning() {
        AutomatedShipPointService.unregisterBonus(BATTLE_CONDITIONING_KEY);
    }

    private CollectiveAutomatedPoints() {}
}
