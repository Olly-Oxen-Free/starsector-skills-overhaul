# PRD: Multi-Tree UI Finalization

- **Slug:** multi-tree-ui
- **Created:** 2026-05-10
- **Status:** draft
- **Spec sources:**
  - `docs/superpowers/specs/skills-overhaul/2026-04-19-skills-overhaul-ui-design.md`
  - `docs/superpowers/specs/skills-overhaul/2026-04-26-skills-overhaul-base-system-refactor.md`

## 1. Summary

Land WIP multi-tree UI refactor (`CharacterTabPanelPlugin.java`, `PathTreeBuilder.java`, `Node.java` — currently uncommitted). Fix two visible regressions discovered in research (lane header label, Metrics.laneCount semantic collision). Implement spec'd active-basics displacement modal (`TODO:1185`). Add multi-tree test coverage (zero UI tests exist today).

## 2. Why

WIP refactor (3→5 slots/tier, multi-tree side-by-side, 8 call sites updated) sits uncommitted on master. Two bugs found in research will produce visible regressions on every tab if shipped as-is. No tests exercise multi-tree path. Active-basics still silently displaces oldest slot — spec calls for a player-choice modal.

## 3. Scope

### Critical bugs (block merge)
- **Lane header labels** — `PathTreeBuilder.java:539` passes `def.getTree()` instead of `lane` to `prettyLaneName`. Every lane shows tree name.
- **`Metrics.laneCount` semantic collision** — `PathTreeBuilder.java:477` sets to `defs.size()` (multi-tree) vs lane count (single-tree). Audit downstream readers.

### WIP commit
- Stage CharacterTabPanelPlugin, PathTreeBuilder, Node + uncommitted JSON (`pilot_systems.json`) + classes.
- Stale comment lines 108-112 update.

### Active-basics modal
- Per spec'd design (T0 confirms exact mechanic). Cancel/confirm-per-slot. ~80-120 LOC. `ActiveLaneService.swap()` already implemented.

### Test coverage
- New classes: `PathTreeBuilderMetricsTest`, `BuildPathsMultiTreeTest`, `SumPendingCostMultiTreeTest`, `CommitPendingPartitionTest`, `LaneColumnKeyspaceTest`, `ActiveBasicsDisplaceTest` (post-modal).

### Audit-only (doc, no code)
- R3 global-static save-reload ordering
- R4 scroll position per-tab vs per-tree
- R5 single-tree factorW change regression check
- R6 off-screen overflow when numTrees large
- R7 `Node.getLane()` "default" footgun

### Out of scope
- New skill/tree content
- TreeRegistry rewrite
- Vanilla character tab co-existence (already replaced)

## 4. Success Criteria

- `./build.sh` + `./build.sh test` pass.
- Lane headers show actual lane names per tab.
- `Metrics.laneCount`/`treeCount` semantically clear; downstream readers updated.
- Active-basics third-T0 click opens chooser modal; cancel preserves state; confirm calls `ActiveLaneService.swap` then rebuilds.
- 5+ new UI tests pass.
- Manual smoke: PILOT (1-tree), COMMANDER (n-tree), ADMIN (n-tree) all render without overlap.

## 5. Risks

- **R1** Lane header label visible regression (must fix pre-merge)
- **R2** Metrics semantic dual-meaning silent miscalc (audit downstream)
- **R3** Global-static metrics — soft reboot / save-reload ordering unverified
- **R4** scroll position shared across trees within a tab (verify intent vs spec)
- **R5** PILOT factorW 3.76→6.12 — node size shrunk; visual regression possible
- **R6** Multi-tree off-screen overflow on small viewport
- **R7** `Node.getLane()` "default" footgun for future code

## 6. Tasks

7 tasks. T0 (spec read) gates T3 modal design. T1 (bug fixes) blocks merge.

## 7. Boundaries

- **require_review:** UI files are gated paths; this PRD is the review.

## 8. Open Questions (resolved in T0)

- Active-basics modal exact UX per UI spec
- Scroll-position model per-tab vs per-tree per spec
- Any additional UI mechanics in 04-26 base-system refactor doc
