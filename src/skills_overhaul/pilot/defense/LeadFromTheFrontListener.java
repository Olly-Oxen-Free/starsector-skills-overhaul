package skills_overhaul.pilot.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.Global;

public final class LeadFromTheFrontListener implements AdvanceableListener {
    private static final float RANGE = 1500f;
    private static final String KEY = "lead_from_front";

    private final ShipAPI flagship;
    private final IntervalUtil interval = new IntervalUtil(0.5f, 0.5f);

    public LeadFromTheFrontListener(ShipAPI flagship) { this.flagship = flagship; }

    @Override
    public void advance(float dt) {
        interval.advance(dt);
        if (!interval.intervalElapsed()) return;

        int alliesInRange = 0;
        for (ShipAPI s : Global.getCombatEngine().getShips()) {
            if (s == flagship || s.getOwner() != flagship.getOwner()) continue;
            if (Misc.getDistance(flagship.getLocation(), s.getLocation()) > RANGE) continue;
            alliesInRange++;
            boolean isFighter = s.getHullSize() == ShipAPI.HullSize.FIGHTER;
            float dr  = isFighter ? 0.03f : 0.05f;
            float dmg = isFighter ? 0.02f : 0.03f;
            MutableShipStatsAPI stats = s.getMutableStats();
            stats.getHullDamageTakenMult().modifyMult(KEY, 1f - dr);
            stats.getArmorDamageTakenMult().modifyMult(KEY, 1f - dr);
            stats.getBallisticWeaponDamageMult().modifyMult(KEY, 1f + dmg);
            stats.getEnergyWeaponDamageMult().modifyMult(KEY, 1f + dmg);
            stats.getMissileWeaponDamageMult().modifyMult(KEY, 1f + dmg);
        }

        float selfBonus = Math.min(0.10f, alliesInRange * 0.01f);
        MutableShipStatsAPI self = flagship.getMutableStats();
        self.getHullDamageTakenMult().modifyMult(KEY + "_self", 1f - selfBonus);
        self.getBallisticWeaponDamageMult().modifyMult(KEY + "_self", 1f + selfBonus);
        self.getEnergyWeaponDamageMult().modifyMult(KEY + "_self", 1f + selfBonus);
        self.getMissileWeaponDamageMult().modifyMult(KEY + "_self", 1f + selfBonus);
    }
}
