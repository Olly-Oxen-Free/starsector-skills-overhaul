package skills_overhaul.commandant;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

public final class CommandFieldListener implements AdvanceableListener {

    public static final String STAT_KEY = "command_field";

    private final ShipAPI ship;
    private final IntervalUtil interval = new IntervalUtil(0.5f, 0.5f);

    public CommandFieldListener(ShipAPI ship) { this.ship = ship; }

    @Override
    public void advance(float amount) {
        if (ship == null || !ship.isAlive()) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        applyTotals(ship.getMutableStats(), CommandFieldService.computeTotalsForShip(ship));
    }

    /** totals[0] = damage bonus, totals[1] = damage-taken reduction. */
    static void applyTotals(MutableShipStatsAPI stats, float[] totals) {
        stats.getEnergyWeaponDamageMult().modifyMult(STAT_KEY, 1f + totals[0]);
        stats.getBallisticWeaponDamageMult().modifyMult(STAT_KEY, 1f + totals[0]);
        stats.getMissileWeaponDamageMult().modifyMult(STAT_KEY, 1f + totals[0]);
        stats.getHullDamageTakenMult().modifyMult(STAT_KEY, 1f - totals[1]);
        stats.getArmorDamageTakenMult().modifyMult(STAT_KEY, 1f - totals[1]);
        stats.getEmpDamageTakenMult().modifyMult(STAT_KEY, 1f - totals[1]);
    }
}
