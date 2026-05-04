package skills_overhaul.pilot.systems;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class NeuralUplinkHullmod extends BaseHullMod {

    public static final String ID = "so_neural_uplink";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize,
                                                MutableShipStatsAPI stats, String id) {
        stats.getTurnAcceleration().modifyPercent(ID, 15f);
        stats.getMaxTurnRate().modifyPercent(ID, 15f);
        stats.getFluxCapacity().modifyPercent(ID, 10f);
        stats.getBallisticWeaponDamageMult().modifyMult(ID, 1.10f);
        stats.getEnergyWeaponDamageMult().modifyMult(ID, 1.10f);
        stats.getMissileWeaponDamageMult().modifyMult(ID, 1.10f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}
