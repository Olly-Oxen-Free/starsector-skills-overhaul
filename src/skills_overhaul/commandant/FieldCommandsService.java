package skills_overhaul.commandant;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.*;

public final class FieldCommandsService {

    public static volatile boolean active = false;

    static final Set<String> lastSnapshot = new HashSet<>();
    static final Map<String, Float> activeBuffs = new HashMap<>();
    static final Set<String> retreatUsedThisBattle = new HashSet<>();

    public static Set<String> diffNewlyIssued(Set<String> last, Set<String> now) {
        Set<String> out = new HashSet<>(now);
        out.removeAll(last);
        return out;
    }

    public static Set<String> tickBuffTimers(Map<String, Float> buffs, float dt) {
        Set<String> expired = new HashSet<>();
        Iterator<Map.Entry<String, Float>> it = buffs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> e = it.next();
            float remaining = e.getValue() - dt;
            if (remaining <= 0f) {
                expired.add(e.getKey());
                it.remove();
            } else {
                e.setValue(remaining);
            }
        }
        return expired;
    }

    public static boolean canApplyRetreat(Set<String> used, String shipId) {
        return !used.contains(shipId);
    }

    public static float bufferDurationFor(String typeName) {
        if (typeName == null) return 0f;
        if (typeName.startsWith("RALLY_")) return 30f;
        if (typeName.equals("RETREAT")) return 10f;
        if (typeName.equals("STRIKE") || typeName.equals("LIGHT_ESCORT")
                || typeName.equals("MEDIUM_ESCORT") || typeName.equals("HEAVY_ESCORT")
                || typeName.equals("ASSAULT") || typeName.equals("ENGAGE")
                || typeName.equals("CAPTURE")) return 20f;
        return 0f;
    }

    public static void tick(float dt) {
        if (!active) return;
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null) return;

        CombatFleetManagerAPI playerMgr = engine.getFleetManager(0);
        if (playerMgr == null) return;
        CombatTaskManagerAPI tm = playerMgr.getTaskManager(false);
        if (tm == null) return;

        Set<String> currentSnapshot = new HashSet<>();
        List<CombatFleetManagerAPI.AssignmentInfo> assignments = tm.getAllAssignments();
        if (assignments == null) assignments = Collections.<CombatFleetManagerAPI.AssignmentInfo>emptyList();

        for (CombatFleetManagerAPI.AssignmentInfo info : assignments) {
            String typeName = info.getType() == null ? "" : info.getType().name();
            List<DeployedFleetMemberAPI> members = info.getAssignedMembers();
            if (members == null) members = Collections.<DeployedFleetMemberAPI>emptyList();
            for (DeployedFleetMemberAPI dfm : members) {
                FleetMemberAPI fm = dfm.getMember();
                if (fm == null) continue;
                String key = typeName + ":" + System.identityHashCode(info) + ":" + fm.getId();
                currentSnapshot.add(key);
            }
        }

        Set<String> newlyIssued = diffNewlyIssued(lastSnapshot, currentSnapshot);

        for (String key : newlyIssued) {
            String typeName = key.split(":", 2)[0];
            float duration = bufferDurationFor(typeName);
            if (duration <= 0f) continue;
            if ("RETREAT".equals(typeName)) {
                String shipId = key.substring(key.lastIndexOf(':') + 1);
                if (!canApplyRetreat(retreatUsedThisBattle, shipId)) continue;
                retreatUsedThisBattle.add(shipId);
            }
            activeBuffs.put(key, duration);
            applyBuffStart(key, typeName);
        }

        Set<String> expired = tickBuffTimers(activeBuffs, dt);
        for (String key : expired) applyBuffEnd(key);

        lastSnapshot.clear();
        lastSnapshot.addAll(currentSnapshot);
    }

    private static void applyBuffStart(String key, String typeName) {
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null) return;
        String memberId = key.substring(key.lastIndexOf(':') + 1);
        for (ShipAPI s : engine.getShips()) {
            FleetMemberAPI fm = s.getFleetMember();
            if (fm == null || !memberId.equals(fm.getId())) continue;
            applyOrderBuff(s, typeName, key);
        }
        CommandFieldService.registerParam("field_command:" + key, "rangeBumpFlat", 500f);
    }

    private static void applyBuffEnd(String key) {
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine != null) {
            for (ShipAPI s : engine.getShips()) {
                MutableShipStatsAPI st = s.getMutableStats();
                st.getMaxSpeed().unmodify(key);
                st.getMaxTurnRate().unmodify(key);
                st.getBallisticWeaponDamageMult().unmodify(key);
                st.getEnergyWeaponDamageMult().unmodify(key);
                st.getFluxDissipation().unmodify(key);
                st.getArmorDamageTakenMult().unmodify(key);
                st.getHullDamageTakenMult().unmodify(key);
                st.getEmpDamageTakenMult().unmodify(key);
                st.getCRPerDeploymentPercent().unmodify(key);
            }
        }
        CommandFieldService.unregisterParam("field_command:" + key);
    }

    private static void applyOrderBuff(ShipAPI ship, String typeName, String key) {
        MutableShipStatsAPI s = ship.getMutableStats();
        if (typeName.startsWith("RALLY_")) {
            s.getCRPerDeploymentPercent().modifyMult(key, 1.10f);
            s.getFluxDissipation().modifyMult(key, 1.10f);
        } else if ("RETREAT".equals(typeName)) {
            s.getHullDamageTakenMult().modifyMult(key, 0.70f);
            s.getArmorDamageTakenMult().modifyMult(key, 0.70f);
            s.getEmpDamageTakenMult().modifyMult(key, 0.70f);
        } else if ("CAPTURE".equals(typeName)) {
            s.getMaxSpeed().modifyMult(key, 1.15f);
        } else if ("ASSAULT".equals(typeName) || "ENGAGE".equals(typeName)) {
            s.getBallisticWeaponDamageMult().modifyMult(key, 1.08f);
            s.getEnergyWeaponDamageMult().modifyMult(key, 1.08f);
        } else if ("STRIKE".equals(typeName)) {
            s.getBallisticWeaponDamageMult().modifyMult(key, 1.20f);
            s.getEnergyWeaponDamageMult().modifyMult(key, 1.20f);
        } else if (typeName.endsWith("_ESCORT")) {
            s.getHullDamageTakenMult().modifyMult(key, 0.85f);
            s.getArmorDamageTakenMult().modifyMult(key, 0.85f);
        }
    }

    public static void resetForBattle() {
        lastSnapshot.clear();
        activeBuffs.clear();
        retreatUsedThisBattle.clear();
    }

    private FieldCommandsService() {}
}
