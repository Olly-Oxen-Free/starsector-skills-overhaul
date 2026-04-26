package skills_overhaul.commandant;

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
        float[] totals = CommandFieldService.computeTotalsForShip(ship);
        ship.getMutableStats().getBeamWeaponDamageMult().modifyMult(STAT_KEY, 1f + totals[0]);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(STAT_KEY, 1f + totals[0]);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult(STAT_KEY, 1f + totals[0]);
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(STAT_KEY, 1f - totals[1]);
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(STAT_KEY, 1f - totals[1]);
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult(STAT_KEY, 1f - totals[1]);
    }
}
