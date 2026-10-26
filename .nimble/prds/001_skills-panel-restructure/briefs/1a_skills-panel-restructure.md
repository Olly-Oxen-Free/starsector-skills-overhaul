# Task brief: 1a Bottom-up panel coordinates and dead UI code purge

PRD: skills-panel-restructure | Wave: 1 | Complexity: 6/10 | Model: opus | Priority: must
Agent: implementer

## Objective
Add a pure, headless coordinate helper (`PanelCoords`) for bottom-up panel-local space and route every y-math site in the Skills Overhaul character-tab panel through it. This fixes the 14 px frame/content offset and the wrong backup hit test. Also delete unreachable merge/sublane code, dead fields/methods, `NodeIconPanelPlugin` and debug text. Behaviour must not change beyond the coordinate fix.

## Context
Mod repo: `workspace/Skills Overhaul/` (Java 8, no JUnit, no compiler checks for game data). PRD "Skills panel restructure" rebuilds the character-tab panel in layers; this is increment 1 (requirements R1, R2). Nothing upstream. Downstream: 2a (SkillTreeModel), 3a (TreeLayout, which reuses `PanelCoords` and bottom-up node geometry), 4a (view).

Problem: engine space has origin bottom-left, y up. `PositionAPI.getY()` is the bottom edge and `InputEventAPI.getY()` is in the same space. `inTL(x, yTop)` pads are top-relative but the resulting `getY()` is still the bottom. The panel code adds top-down offsets to `getY()` and mixes conventions. Arithmetic (not measured): the window quad lands at bottom-up `[28, h-14]` while `inTL` content assumes `[14, h-28]`, a 14 px offset. Cursor conversion `eventToPanelLocalY` = `(screenH - e.getY()) - py` equals the true value only when the panel is vertically centred (`inMid()`, `CharacterTabReplacer.java:203`), so tooltip and wheel math work only by accident. The backup hit test `pickNodeAtPanelLocal` compares top-down `ly` to bottom-up `nodeLocal`. Clicks work today because the primary path uses `PositionAPI.containsEvent` (`:1022-1026`), which is correct.

## Requirements
- R1: one pure helper converts to bottom-up panel-local coordinates. Convention everywhere: draw at `py + localY`; convert `inTL(x, yTop)` as `localY = panelH - yTop - h`; cursor `localY = event.getY() - py`; rect containment in the same space. `PanelCoords` must not import `Global` or GL so it runs headless.
- R2: remove dead code and debug text (list under Success criteria).
- Node geometry in `PathTreeBuilder`: `emitNode` stores top-down `nodeLocal` (`:686-687`), while `syncNodeGeometryFromResolvedPositions` overwrites it bottom-up (`:541-544`). Make both bottom-up.
- Keep the "Keys 1-3 switch tabs" hint.
- `Node.getLane/isMerge/getParentLanes` stay (`NodeSlotTest` pins them); only UI code that branches on them goes.

## Success criteria
- [ ] New PanelCoordsTest passes under ./build.sh test: inTL conversion (localY = panelH - yTop - h), cursor conversion (event.getY() - py) and rect containment hold for a panel whose bottom edge is not at screenH/2 - h/2 (non-centred)
- [ ] CharacterTabPanelPlugin y-sites (winTop :514, pTop :547, barTop :727, trackTop :769, scissor :590-592, tooltip ty :365, wheel region :1111-1112, eventToPanelLocalY and pickNodeAtPanelLocal :994-1018) and ConnectionLineDrawer use PanelCoords; cursor y conversion (eventToPanelLocalY or its PanelCoords replacement) returns e.getY() - py with no screen-height flip, and the only getScreenHeight() call left in CharacterTabPanelPlugin is the scissor viewport scale
- [ ] NodeIconPanelPlugin.java is deleted; emitSublaneColumn, isCrossLane, SUBLANE_W, merge branches, configureForViewport(TreeDef,...), resolveTreeOf, LaneColumn, LANE_FRAME, LANE_FRAME_DIM, HEADER_BG, bbLevel/bbPoints, deprecated 4-arg ConnectionLineDrawer.draw with its flipY branch and the empty render() override are gone (grep returns nothing)
- [ ] UI_VERSION and its display/log lines, the 'processInput is live' and 'attach ... jar=' logs are gone; 'Free Uni'/'Mig Pool' read 'Free universal points'/'Migrated points'
- [ ] ./build.sh and ./build.sh test pass; NodeSlotTest is unchanged

## Files to modify
Line numbers are from research and may drift; locate by symbol.

| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/PanelCoords.java` | create | pure static helpers: inTL to localY, cursor to local, rect contains |
| `test/skills_overhaul/ui/PanelCoordsTest.java` | create | headless test, `main()` + `assert`; new dir `test/skills_overhaul/ui/` |
| `src/skills_overhaul/ui/CharacterTabPanelPlugin.java` | modify | route y-sites through PanelCoords; drop bbLevel/bbPoints (`:475-476`), empty `render()` (`:818`), `UI_VERSION` display (`:484`), 'processInput is live' log (`:1043`); rename labels (`:421-422`); leave `hasAnyInPreviousTier` (2a owns it) |
| `src/skills_overhaul/ui/PathTreeBuilder.java` | modify | bottom-up node geometry; remove `emitSublaneColumn` (`:613`), merge branches (`:580,623,650,715,758,770`), `isCrossLane` (`:740`), `SUBLANE_W` (used `:80,145,166,187,215,308+`), `merge:` label (`:793`), `configureForViewport(TreeDef,...)` (`:94-225`), `resolveTreeOf` (`:784`), `LaneColumn` (written `:460`, never read), `LANE_FRAME`, `LANE_FRAME_DIM`, `HEADER_BG` (`:64-66`), `UI_VERSION` (`:45`) and its logs (`:117,122,127`) |
| `src/skills_overhaul/ui/ConnectionLineDrawer.java` | modify | use PanelCoords; remove deprecated 4-arg `draw` and `flipY=true` branch; fix top-down doc comment (`:40-44`) |
| `src/skills_overhaul/ui/CharacterTabReplacer.java` | modify | remove `UI_VERSION` log (`:214`) and the 'attach ... jar=' logs (`:114-129` region, if located here or in the plugin `attach`) |
| `src/skills_overhaul/ui/NodeIconPanelPlugin.java` | delete | zero callers |

## Deliverables
| Artifact | Path | Done when |
|---|---|---|
| Coordinate helper with headless test | `src/skills_overhaul/ui/PanelCoords.java, test/skills_overhaul/ui/PanelCoordsTest.java` | PanelCoordsTest runs under ./build.sh test and covers a non-centred panel |
| Manual in-game verification (user) | `.nimble/prds/skills-panel-restructure/PRD_skills-panel-restructure.md §8 M1` | user confirms frame, XP bar, scrollbar and portrait border align with content and node clicks/tooltips still work |

The in-game check is the user's; do not claim it passed. State in the report that it is pending.

## Implementation guidance
- Pattern to follow: `test/skills_overhaul/tree/NodeSlotTest.java` (package line, `public static void main(String[] args)`, `assert` with messages, ends `System.out.println("ALL PASSED")`). Also match `test/skills_overhaul/tree/PrereqValidatorTest.java` for style. `./build.sh test` compiles `src`+`test` with `-source 8 -target 8`, runs every `*Test.class` with `java -ea`, and stops on first failure; a new `*Test` with `main` self-registers.
- Research (internal Q2): wrong comment at `CharacterTabPanelPlugin.java:511-513`; wrong math at `winTop :514`, `pTop :547`, `barTop :727`, `trackTop :769`, scissor `:590-592`. GL icon drawing (`:620-633`) is already correct after frame 1 because it uses bottom-up `getCenterY()-py0`. Tooltip `ty` (`:365`) and wheel region (`:1111-1112`) depend on `eventToPanelLocalY` (`:994-1005`). All sites and `ConnectionLineDrawer` change together.
- Research (internal Q1): `PathTreeBuilder` writes the plugin's package-private maps (`edges, nodeCenters, nodeLocal, nodePositions, nodeHitById, ...`); keep those shapes. The scissor still needs `getScreenHeight()` for the viewport scale; that is the only allowed remaining call. `configureForViewport(List<TreeDef>,...)` is the live overload (called `CharacterTabPanelPlugin.java:221`); keep it.
- Remove imports and helpers orphaned by your deletions only. Do not tidy other code, and do not touch the ten `public static float` metrics beyond removing `SUBLANE_W`.
- Test cases for `PanelCoordsTest`: pick panel at `py` not equal to `screenH/2 - h/2` (e.g. screenH 1080, panel h 600, py 100) and assert: `inTL(yTop=14, h=30)` gives `panelH-14-30`; cursor `e.getY()=250` gives `150`; contains true inside / false just outside both edges (bottom edge inclusive/exclusive as you define and document); a centred panel and a non-centred panel give different results for the old formula but the same, correct result for the new one.
- Edge cases: panel taller than content, `yTop=0`, `h=0`, negative local results (return as is, no clamping).
- Verification: grep each removed symbol across `src` and `test` (`grep -rn`), expect nothing. `grep -n getScreenHeight src/skills_overhaul/ui/CharacterTabPanelPlugin.java` should show only the scissor use.
- No compiler checks game data; this task changes no JSON/CSV.

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08):
- Campaign listeners: `addListener(l, true)` means transient (not saved); `false` saves a new copy every load.
- `./build.sh` builds the jar; `./build.sh test` runs 77 test classes.
- Status: 2026-10-02 audit fixes are compile/unit-test verified only; in-game checks are pending.
- The UI classes (`CharacterTabPanelPlugin`, `PathTreeBuilder`, `CharacterTabReplacer`, `ConnectionLineDrawer`, `NodeIconPanelPlugin`, `SkillUISounds`, `ReflectionUtils`) belong to this theme.

## Testing requirements
Run `validate.sh --changed` (targeted tests), then the full `./build.sh test` once at the end. Add exactly one new test class, `PanelCoordsTest`, as specified above. `NodeSlotTest` must pass unchanged. There is no JUnit: each `*Test` runs its `main()` with `assert` under Java 8. Rendering and clicks cannot run headless; the in-game check (M1) is the user's. Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar. `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: do not commit that file (restore it with `git checkout -- jars/SkillsOverhaul.jar` before committing, or leave it unstaged). Do not commit unless the caller asks.
- Flag in the commit body if modified: `build.sh`, `mod_info.json` (neither should be touched).
- Out of scope: SkillTreeModel / tier-gate / commit changes (2a), TreeLayout and zoom (3a), tooltips (3b/4a), host or reflection changes (5x), `Node`/`TreeDef` APIs, tree JSON, `SpendService`, `zealot/CrusadeNPCStub`, colour palette, increment 6 items. Pre-existing dead code you notice that is not listed: mention it, do not delete.

## Execution
- Branch: `worktree/skills-panel-restructure-1a`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
