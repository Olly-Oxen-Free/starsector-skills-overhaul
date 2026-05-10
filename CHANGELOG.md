# Changelog

## Unreleased — Defense lane spec gap closure (2026-05-10)

- Iron Bastion (T3): now applies +20% RoF + 25% weapon turn rate while taking active damage.
- Flux Fortress (T4): shield damage taken scales with flux level (0 → −30% at 100%).
- Overwhelming Force (T4): +25% flux dissipation + 20% hard-flux dissipation flat while advancing/shielded.
- Adamantine (capstone): post-discharge window grants −25% EMP taken + +8% hull regen over 5s; real EMP arc spawned (300 energy + 1500 EMP); buffer recharges as flux climbs (was stale).
- Orphan skills (off-tree): Impact Mitigation + Carrier Supremacy implemented but not in tree (5-slot grid full). Grant via Console Commands:
  ```
  runcode skills_overhaul.console.OrphanSkillGranter.grant("impact_mitigation");
  runcode skills_overhaul.console.OrphanSkillGranter.grant("carrier_supremacy");
  runcode skills_overhaul.console.OrphanSkillGranter.revoke("<name>");
  ```
  Persists in MemoryAPI; re-applied on game load. Note: `impact_mitigation` params (`hitChance`, `damageReduction`) require SOPilotedStatHandler extension (defense T4) before they apply.

## v0.2 — Character tab UI takeover (2026-04-20)

- Replaced v0.1 dialog stopgap with full character-tab takeover.
- New 3-tab panel: PILOT / COMMANDER / ADMIN.
- 48×48 node buttons on a 3-lane × (L/M/R) slot grid per tier.
- Restored merge nodes with cross-lane prereqs.
- Pending-purchase + confirm flow (vanilla parity). Esc clears.
- GL_LINES prereq edge drawing with state colors (teal/amber/gray/stippled-green).
- Schema: added `slot` field + `parentLanes` for merge nodes.
- Tests: NodeSlotTest, MergeNodeTest, PendingPurchaseTopoSortTest added.
PrereqValidatorTest + TreeLoaderRealJSONTest extended.

## 0.1.0 (unreleased)

- v1: three-tree skill overhaul (Pilot/Commander/Admin), activity-gated XP, custom UI, save-compat migration.