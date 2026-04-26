# Changelog

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