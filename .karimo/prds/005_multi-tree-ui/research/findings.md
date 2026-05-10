# Research Findings — multi-tree-ui

**Note:** Researcher reported spec dir missing — incorrect. UI spec exists at `docs/superpowers/specs/skills-overhaul/2026-04-19-skills-overhaul-ui-design.md` and base-system refactor at `2026-04-26-skills-overhaul-base-system-refactor.md`. **Spec review is a sub-task in T0.**

## WIP diff summary

### `PathTreeBuilder.java`
- Layout const `LANE_CONTENT_W` changed `3f*NODE+2f*SLOT_GAP_X` → `5f*NODE+4f*SLOT_GAP_X` (3→5 slots/tier). 11 sites.
- Width factor formula `factorW = 6.12*laneCount + 1.80*sublaneCount` (was 3.76). Comment at lines 108-112 stale, refs old 3.76.
- New overload `configureForViewport(List<TreeDef>, float, float)` lines 261-346 — multi-tree side-by-side. Computes nodeSize from `usableW/(6.12*numTrees+0.80*(numTrees-1))`. Two-pass correction + 16-iter convergence.
- `buildPaths` lines 388-477 — iterates defs, advances `treeCursor` by `2f*LANE_GAP_X` between trees. Lane keys namespaced `"tree{di}:{lane}"` to avoid collision (line 449).
- **BUG: `emitLaneColumn` line 539** passes `def.getTree()` to `prettyLaneName` instead of `lane`. Result: every lane in a tree shows the tree name as label. Visible regression on every tab.
- **BUG: `Metrics.laneCount` line 477** — set to `defs.size()` (= tree count) in multi-tree path; means lane count in single-tree path. Semantic collision.

### `CharacterTabPanelPlugin.java`
8 call sites updated: layout metrics (220-232), countPurchasedNodes (265-274), sumPendingCost (925-931 + `findTreeOf` null-safe), click handler T0 branch (1165-1169), pending-purchase branch (1201-1232 — also expanded budget pools `freeUni + migLocked + migPool + POOL_UNALLOC`), `commitPending` per-def topo partition (1238-1265), `removePendingWithDependents` (1276-1281), helper `f(Object)` (1238).

**TODO line 1185-1186:** active-basics displacement v0 silently displaces `active.get(0)`. Spec'd follow-up: modal panel listing active T0s, cancel/confirm-per-slot, calls `ActiveLaneService.swap()` (already implemented).

### `Node.java`
- `getLane()` now returns `"default"` (was null). Backward-compat stub — any future multi-lane code reading `getLane()` sees one bucket. UI derives lanes from x-coord via `deriveLanes(def)`, so currently harmless. Footgun.
- `fromJSON` default icon `"graphics/icons/skills/combat.png"` (was null). Hides missing-icon errors.

## Test coverage

**Zero UI tests.** No `test/skills_overhaul/ui/` directory.

**Valid post-refactor:** `TreeLoaderTest`, `TreeRegistryTest`, `GridValidationTest`, `PrereqValidatorTest`, `PrereqClauseTest`, `PendingPurchaseTopoSortTest`, `ActiveLaneServiceTest`, `NodeSlotTest`.

**Stale/missing:**
- `TreeLoaderRealJSONTest` needs Global init; doesn't exercise `loadAllForTab` (multi-tree index path).
- No `PathTreeBuilderMetricsTest` (multi-tree width/height bounds).
- No `BuildPathsMultiTreeTest` (lane key uniqueness across defs).
- No `SumPendingCostMultiTreeTest`.
- No `CommitPendingPartitionTest` (per-def topo sort).
- No active-basics displacement test.
- No `LaneColumnKeyspaceTest`.

## Reference mods

- **LunaLib** — settings/refit-only. No tab-tree primitives. No applicable patterns.
- **Second-in-Command** — single-list officer skills via `CustomPanelAPI`. No multi-column composite layout. No applicable patterns.

Starsector UI is self-contained (`CustomPanelAPI`/`TooltipMakerAPI`); no external widget toolkit.

## Risks

- **R1** Lane header label bug — visible regression every tab. Block merge.
- **R2** `Metrics.laneCount` dual meaning — silent miscalculation if downstream reads it.
- **R3** Global-static metrics race — `PathTreeBuilder` metrics process-global; soft reboot / save-reload ordering not validated.
- **R4** `scrollXByTree` `EnumMap<Tree,...>` keyed by tab enum, not per-`TreeDef`. Trees within a tab share scroll position. Check if intentional.
- **R5** Single-tree path implicitly used for tabs with 1 tree (PILOT) — but factorW changed from 3.76 → 6.12. Nodes smaller than before. No regression test.
- **R6** Off-screen overflow when numTrees large — 16-iter convergence with min nodeSize=36px may exit overflowing on small viewports. No post-clamp guard.
- **R7** `Node.getLane()` returning `"default"` for all — silent footgun for future code reading lane from Node.

## Recommended task slicing — 7 tasks

1. **T0** — Read UI spec (`2026-04-19-skills-overhaul-ui-design.md` + `2026-04-26-skills-overhaul-base-system-refactor.md`). Document spec'd active-basics displacement design + scroll model + tab navigation. Reconcile with current WIP.
2. **T1** — Fix lane header label bug (R1) + Metrics.laneCount semantic split (R2). Two-line + small refactor.
3. **T2** — Verify single-tree path post-factor-change (R5) — load PILOT tab in-game, confirm node size acceptable. Adjust factorW/factorH if regressed.
4. **T3** — Active-basics displacement modal (TODO line 1185). Per spec from T0. ~80-120 LOC. `ActiveLaneService.swap()` already implemented.
5. **T4** — PathTreeBuilder metrics + buildPaths multi-tree unit tests (5 new test classes per slicing list).
6. **T5** — Purchase-flow multi-tree tests (commitPending partition, sumPendingCost, removeWithDependents).
7. **T6** — Land WIP commit + smoke test (build, in-game tab walkthrough). Address stale comment lines 108-112 + R3/R4/R6/R7 doc-only audit notes.

## Cited sources

- `src/skills_overhaul/ui/CharacterTabPanelPlugin.java:220-1281` (8 call sites) + `:1185-1186` TODO
- `src/skills_overhaul/ui/PathTreeBuilder.java:108-539` + `:261-346` overload + `:388-477` buildPaths
- `src/skills_overhaul/tree/Node.java:41, :73`
- `src/skills_overhaul/tree/TreeRegistry.java`, `TreeLoader.java`
- `src/skills_overhaul/tree/ActiveLaneService.java:17`
- `test/skills_overhaul/` (71 files, 0 UI)
- Spec (researcher missed; reviewer to consume in T0): `docs/superpowers/specs/skills-overhaul/2026-04-19-skills-overhaul-ui-design.md`, `2026-04-26-skills-overhaul-base-system-refactor.md`
