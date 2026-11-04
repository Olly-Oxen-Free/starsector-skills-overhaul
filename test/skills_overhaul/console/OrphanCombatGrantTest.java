package skills_overhaul.console;

import skills_overhaul.effect.EffectRegistry;
import skills_overhaul.effect.PilotShipStatEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** reviews/2026-11-04-0434-console.md B1: orphan grants must reach the per-combat ship pass. */
public class OrphanCombatGrantTest {
    public static void main(String[] args) {
        final Map<String, Object> mem = new HashMap<String, Object>();

        // Nothing granted: the combat pass sees no orphans.
        assert OrphanSkillGranter.granted(k -> Boolean.TRUE.equals(mem.get(k))).isEmpty();

        // Both documented orphans granted.
        mem.put("$so_orphan_impact_mitigation", Boolean.TRUE);
        mem.put("$so_orphan_carrier_supremacy", Boolean.TRUE);
        List<OrphanSkillGranter.OrphanSpec> g = OrphanSkillGranter.granted(k -> Boolean.TRUE.equals(mem.get(k)));
        assert g.size() == 2 : "both granted orphans must be listed, got " + g.size();
        assert g.get(0).nodeId.equals("so_defense_impact_mitigation");
        assert g.get(0).params.optDouble("damageReduction", 0) == 0.25;
        assert g.get(1).nodeId.equals("so_defense_carrier_supremacy");

        // Each one's handler must be a per-combat ship-stat effect, or
        // SkillsOverhaulCombatPlugin cannot apply it to the flagship.
        for (OrphanSkillGranter.OrphanSpec x : g) {
            assert EffectRegistry.get(x.effectType) instanceof PilotShipStatEffect
                    : x.effectType + " is not applied per combat";
        }

        // Revoked (false) is not granted.
        mem.put("$so_orphan_carrier_supremacy", Boolean.FALSE);
        assert OrphanSkillGranter.granted(k -> Boolean.TRUE.equals(mem.get(k))).size() == 1;

        System.out.println("ALL PASSED");
    }
}
