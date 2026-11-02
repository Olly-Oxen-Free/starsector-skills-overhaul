# Task brief: 4a SkillTreeView built once per tab, with addTooltipTo tooltips

PRD: skills-panel-restructure | Wave: 4 | Complexity: 7/10 | Model: opus | Priority: must
Agent: implementer

## Objective
Replace the 1300-line `CharacterTabPanelPlugin` and the 810-line `PathTreeBuilder` with one `SkillTreeView` (a `CustomUIPanelPlugin`) that builds its child components once per construction and per tab switch, and drives all state from the headless `SkillTreeModel`, `TreeLayout` and `GameSpend.commit` built in waves 2-3. This delivers native tooltips, zoom and pan, an all-or-nothing confirm and a red rejection flash, and removes the per-frame rebuild work.

## Context
Mod: Skills Overhaul (Starsector, Java 8, repo root = `workspace/Skills Overhaul/`). The CHARACTER-tab skill panel today fuses state, layout, drawing and input in one plugin, mixes top-down and bottom-up y math, rebuilds edges, textures and tooltips every frame, and commits purchases node by node (partial commits possible). This task is increment 4 of 5 of the panel restructure and fulfils R3-R7:
- R4 all-or-nothing confirm with red flash; R5 zoom about cursor, clamped pan, correct hit tests when zoomed; R6 native `addTooltipTo` tooltips (name, cost, effects, prereq met/unmet); R7 view built once per tab switch, cached sprites, no per-frame rebuild.

Builds on (already merged when this task runs; READ their real signatures, do not guess):
- 1a: pure bottom-up coordinate helper `PanelCoords` (`ui/PanelCoords.java` or nearby; find with Glob) and the purge of dead code. `getY()` of a `PositionAPI` is the BOTTOM edge and y grows up; panel-local cursor y = `event.getY() - py`, no screen-height flip. `ConnectionLineDrawer` already uses `PanelCoords`.
- 2a: `ui/model/SkillTreeModel` + `NodeStatus` (OWNED / PENDING / UNLOCKABLE / BLOCKED with reason PREREQ, TIER, BUDGET(short n)); pending add/remove returning a rejection with reason (add of unaffordable/tier-blocked node leaves pending unchanged; remove drops pending dependents); `level/PurchasePlan`; `GameSpend.commit(Tree, Collection<String>)` which dry-runs the whole batch on one cloned whole-tab snapshot, writes nothing on failure, names the failing node, plays no sound.
- 3a: `ui/layout/TreeLayout` instance (node rects and edge endpoints in bottom-up content space, fit-to-viewport, zoom clamped to [fit, 2.0], pan clamped, `zoomAt(cx,cy)`, `screenToContent`/`contentToScreen`, `hitTest`).
- 3b: `ui/view/NodeTooltipContent`, `ui/view/NodeTooltip` (a `TooltipCreator`, explicit width, not expandable) and `ui/view/UiColors` (current palette: owned, pending, unlockable, locked, capstone).

Downstream: 5a/5b (wave 5) add `CharTabHost` and `DialogHost`, which both mount `SkillTreeView`; 5c removes `CharacterTabReplacer`. Keep the view host-agnostic: it takes a viewport size and is mounted by someone else. Until increment 5, `CharacterTabReplacer` mounts it.

## Requirements
Create `SkillTreeView` and wire everything:
1. Build once: child components (node anchor panels, buttons, header elements) are created in the constructor and on tab switch only. No `removeComponent`/`addComponent`, `loadTexture` or edge rebuild in `render`, `renderBelow`, `advance` or `processInput` per frame. Hover changes do not rebuild.
2. State: one `SkillTreeModel` per current tab (Pilot / Commander / Admin) over `TreeRegistry.getTrees(tab)`; edges and node colours come from model status plus `TreeLayout` geometry; colours from `UiColors`.
3. Tooltips: for each node create a small `CustomPanelAPI` child positioned over the node (anchor), and attach `NodeTooltip` with `TooltipMakerAPI.addTooltipTo(tc, anchor, TooltipLocation.X)`. Children do NOT follow `glTranslatef/glScalef`: on zoom or pan change, reposition them with `PanelCoords` + `TreeLayout.contentToScreen`; never rebuild. Tooltip content must reflect live state (use the `recreateEveryFrame` overload or reposition-only as appropriate; if the tooltip needs current model state, hand it the node id and read the model at show time).
4. Sprites: cache `SpriteAPI` per icon path in a field map; call `Global.getSettings().loadTexture(path)` only when `getSprite(path).getTextureId() <= 0`. Set `sprite.setAlphaMult(alphaMult * x)` inside `render` every frame (shared-instance alpha is an open gap).
5. Input: wheel zooms about the cursor (`zoomAt`); left-drag over 6 px pans; a shorter press is a click that toggles pending (add via model, remove via model). Hit tests and tooltip anchors use the inverse transform, correct at non-fit zoom and pan. Click of an unaffordable or tier-blocked node: red flash on that node (~0.4 s), pending unchanged.
6. Confirm button: call `GameSpend.commit` once. On success: play exactly one sound (via `SkillUISounds.playTierSkillSound`, one call per successful commit), clear pending, refresh model snapshot and node states (no full rebuild needed beyond status refresh). On failure: nothing bought, flash the offending node (named by the commit result) red for ~0.4 s.
7. Keys: Esc clears pending (consume only when pending exists, as today); keys 1-3 switch Pilot/Commander/Admin tab (a tab switch rebuilds children once and clears pending). Pending is also cleared when the panel is discarded (spec 2.1).
8. Owned-T0 click keeps today's active-slot toggle: the purchased-node branch of `CharacterTabPanelPlugin`'s node-click handler (marked `TODO(active-basics)`, near line 1187 before 1a) calling `ActiveLaneService`, including its auto-displacement. Copy that behaviour; increment 6 replaces it.
9. Keep the existing header (portrait, XP bar, points, respec and confirm buttons) and the current palette. Labels "Free Uni" and "Mig Pool" become "Free universal points" and "Migrated points" (check whether 1a already renamed them). "Keys 1-3 switch tabs" hint stays. No debug/version text.
10. Delete `ui/CharacterTabPanelPlugin.java` and `ui/PathTreeBuilder.java`. Update `CharacterTabReplacer` to construct and mount `SkillTreeView` (it currently calls `PathTreeBuilder.computeMetrics` at about `:184` and `new CharacterTabPanelPlugin()` at about `:199`, and logs `CharacterTabPanelPlugin.UI_VERSION` at about `:214`; drop those uses). Keep its parking/mount/uninstall logic otherwise unchanged. Discard of the panel in the replacer (about `:299-302`) must lead to pending being cleared.
11. `GameSpend`: remove `import skills_overhaul.ui.SkillUISounds` and the sound call in `GameSpend.purchase` so `level/` no longer imports `ui/`. Keep `purchase`, `purchasedKeys(TreeDef)` (package-private, used by `GameSpendSnapshotTest`) and everything else working.
12. `ConnectionLineDrawer`: adapt only as needed to draw edges from `TreeLayout` endpoints/model status (it already takes bottom-up coords after 1a).

## Success criteria
- [ ] SkillTreeView builds child components only on construction and tab switch; no removeComponent/addComponent, loadTexture or edge rebuild runs per frame (grep of render/renderBelow/advance confirms) and hover changes do not rebuild
- [ ] Confirm calls GameSpend.commit once; a failed commit buys nothing and flashes the offending node red for about 0.4 s; adding an unaffordable or tier-blocked node also flashes red; a successful commit plays one sound
- [ ] Mouse wheel zooms about the cursor, left-drag over 6 px pans, a shorter press toggles pending; hit tests and tooltip anchors stay correct at non-fit zoom and pan
- [ ] CharacterTabPanelPlugin.java and PathTreeBuilder.java are deleted; GameSpend no longer imports skills_overhaul.ui; Esc clears pending, keys 1-3 switch tab, and owned-T0 click keeps the active-slot toggle
- [ ] ./build.sh and ./build.sh test pass; manual verification steps for M4 are listed in the PR description

## Files to modify
Run Glob to confirm action at start; expected state:
| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/view/SkillTreeView.java` | create | the new view (dir `ui/view/` exists from 3b) |
| `src/skills_overhaul/ui/ConnectionLineDrawer.java` | modify | draw from TreeLayout/model status |
| `src/skills_overhaul/ui/CharacterTabReplacer.java` | modify | mount SkillTreeView, drop PathTreeBuilder/plugin uses |
| `src/skills_overhaul/ui/CharacterTabPanelPlugin.java` | delete | replaced by SkillTreeView |
| `src/skills_overhaul/ui/PathTreeBuilder.java` | delete | replaced by TreeLayout + view |
| `src/skills_overhaul/level/GameSpend.java` | modify | drop ui import and sound from `purchase` |

## Deliverables
| Artifact | Path | Done when |
|---|---|---|
| Manual in-game verification (user) | `.nimble/prds/001_skills-panel-restructure/PRD_skills-panel-restructure.md` section 8 M4 | user confirms tooltips appear on hover, zoom/pan keep clicks on target, an over-budget confirm buys nothing and flashes red, and frame time does not drop with the panel open |

(List the M4 manual steps in the PR description: open CHARACTER; hover a node for tooltip; wheel zoom and drag pan then click and hover nodes; queue an over-budget batch and confirm, expect no purchase and a red flash; Esc clears pending; keys 1-3 switch tab; click an owned T0 node and see the active-slot toggle; check frame time.)

## Implementation guidance
- Pattern to follow for the CustomUIPanelPlugin shape, bottom-up quads, hit tests: the vanilla `com/fs/starfarer/api/impl/campaign/ExampleCustomUIPanel.java` (in `game/starfarer.api.zip`), and the existing `CharacterTabPanelPlugin` (read it fully BEFORE deleting; it holds header, input, scissor, scroll and active-lane logic you must port). Also `ui/CharacterTabReplacer.java` for how the panel is created and mounted.
- Tooltips: `TooltipMakerAPI.addTooltipTo(TooltipCreator, UIComponentAPI, TooltipLocation)` and the overload with `boolean recreateEveryFrame` (API `TooltipMakerAPI.java:333-334`) accept any `UIComponentAPI`, including a `CustomPanelAPI` child (SiC: `AptitudeSkillBarElement.kt:68-71`). Width comes from `getTooltipWidth`. Untested gaps: whether a tooltip on a clipped child shows, and whether it survives remove/re-add; verify in-game.
- Zoom/pan: no vanilla helper and no mod precedent. Use `GL11.glPushMatrix(); glTranslatef; glScalef` in render from the `TreeLayout` transform, invert for hit tests on `event.getX()/getY()` (same space as `position`). GL scissor clip to the panel exists in the old plugin (scissor at about `:590-592`, uses the only allowed `getScreenHeight()` call); keep it, scaled by screen height only. UI children do not follow the GL transform; reposition them yourself.
- Sprites: `SettingsAPI.getSprite(path)` needs the file already loaded; load-if-missing pattern is `SpriteAPI s = getSprite(path); if (s.getTextureId() <= 0) loadTexture(path);` (loadTexture throws IOException). Cache in a `Map<String,SpriteAPI>` field.
- Input: `InputEventAPI.isMouseScrollEvent()` and the sign of `getEventValue()` for the wheel; the old plugin's wheel handling (about `:1111-1112`) shows the sign and the region test. Consume events the old plugin consumed; do not consume mouse-move.
- Old per-frame culprits to NOT carry over: `refreshEdgesAfterLayout` (scans memory, calls `canPurchaseWithPending` per node and edge), `ActiveLaneService.getActive` per frame, `loadTexture`+`getSprite` per node per frame, tooltip element removed/re-added on hover, `rebuild()` on wheel. Compute active-lane state only on refresh events (construction, tab switch, commit, toggle).
- Headless layers (model, layout, content) must stay free of `Global`/GL; all game API use lives in the view.
- Budget logic comes from the model/`SpendService`; do not reimplement the old four-pool `totalPts`/`sumPendingCost` check.
- `ReflectionUtils.getChildrenCopy` swallows throwables into an empty list; do not rely on it for correctness.
- Edge cases: empty tab or missing TreeDef (render header only, no throw); zero-size viewport; commit with empty pending (button no-op); flash timers advance in `advance(amount)` (not frames); two flashes at once; tab switch during a flash (clear flashes); commit where `NodeEffectApplier.apply` throws is handled inside `GameSpend.commit` (logs ERROR, continues).
- Line numbers cited above come from before 1a and drift; locate by symbol.
- Research: `research/external/findings.md` Q5, Q6, Q7; `research/internal/findings.md` Q1, Q7, Q8 (facts above already carry the relevant content).

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08):
- Campaign listeners: `addListener(l, true)` means transient (not saved); `false` saves a new copy every load. `AdminXPTracker` and `SupplyBufferRenderer` were persistent and stacked until fixed.
- Order on load: migration first, then suppression, then reapply, then orphan grants.
- `./build.sh` builds the jar; `./build.sh test` runs the test classes.
- Status: 2026-10-02 audit fixes are compile/unit-test verified only; in-game checks are pending.

From `brain/wiki/skills-overhaul-skill-trees.md` (verified date not copied; entry has no stale flag):
- Effect handler types use the `SO_` prefix and vanilla param vocabulary; shared handlers (e.g. `SO_fleet_stat`) are reused across trees.
- Admin trees (Warden, Merchant, Prospector, Steward) are placeholders, so an Admin tab may have empty or placeholder TreeDefs; the view must cope.

## Upstream findings
None recorded (`findings.md` absent at brief time).

## Testing requirements
Run `validate.sh --changed` (targeted tests). Add tests only where named here: none are named for this task; the view needs the game and cannot run headless. Keep all logic that can be headless in the model/layout/content classes from waves 2-3. Final gate: `./build.sh test` (compiles `src` and `test` with Java 8, then runs each `*Test` class's `main()` with `java -ea`, so assertions are `assert`; there is no JUnit; any failure aborts the run). `GameSpendSnapshotTest` must still pass (it uses package-private `GameSpend.purchasedKeys(TreeDef)`). Deleting the two UI classes must not break any test; grep `test/` for references first (only `GameSpendSnapshotTest` mentions `GameSpend`). Verify no per-frame rebuild by grepping `render`, `renderBelow`, `advance` of `SkillTreeView` for `removeComponent`, `addComponent`, `loadTexture` and edge rebuild calls, and report the grep.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar by hand.
- Commit: commit your work on the task branch per the implementer contract (conventional subject, files staged by explicit path). `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: never stage or commit it; run `git checkout -- jars/SkillsOverhaul.jar` before each commit.
- Leave `.serena/` and unrelated working-tree changes alone.
- Also off limits per PRD: `data/config/skills_overhaul/trees/**`, `src/skills_overhaul/tree/Node.java`, `TreeDef.java`, `src/skills_overhaul/level/SpendService.java`, `src/skills_overhaul/zealot/CrusadeNPCStub.java`.
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (this task should not touch them).
- Out of scope: `CharTabHost`, `DialogHost`, probe, hotkey (wave 5); removing `CharacterTabReplacer` (5c); increment 6 (failed-tree label, choosing which T0 to displace, real respec cost); spec 4.1 colour palette, fixed 48x48 nodes, lanes, hybrids, merge nodes; changing `Node`/`TreeDef` APIs or tree JSON; `SpendService` pool order.

## Execution
- Branch: `worktree/skills-panel-restructure-4a`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
- Gaming guard: run `~/.claude/hooks/gaming-check.sh` before the first build or test and before each later build; exit 1 means the owner is gaming: stop, commit WIP, and report `paused: gaming` instead of retrying.
