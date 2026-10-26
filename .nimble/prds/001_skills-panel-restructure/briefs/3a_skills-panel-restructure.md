# Task brief: 3a TreeLayout with zoom and pan

PRD: skills-panel-restructure | Wave: 3 | Complexity: 5/10 | Model: opus | Priority: must
Agent: implementer

## Objective
Add `TreeLayout`, a pure, instance-based class that lays out the nodes of a tab's `TreeDef`s in a bottom-up content space, fits them to a viewport, and holds zoom and pan state with clamps and an inverse transform for hit tests. It replaces `PathTreeBuilder`'s static metrics once the view (4a) lands. It carries no Global or GL dependency, so `./build.sh test` can check it headlessly.

## Context
The Skills Overhaul mod (Starsector, Java 8, no JUnit) has a character-tab skills panel being rebuilt into model, layout, view and host layers. Today `PathTreeBuilder` (`src/skills_overhaul/ui/PathTreeBuilder.java`) keeps its metrics in ten `public static float` fields and mixes top-down and bottom-up y math. This task fulfils requirement R5 ("`TreeLayout` with fit, zoom about cursor and clamped pan"). Nothing in the old UI is switched over here: increment 4 does that.

- Upstream: 1a added `PanelCoords`, a pure bottom-up panel-local coordinate helper (look in `src/skills_overhaul/ui/`; use its conventions and do not duplicate it). 2a added `SkillTreeModel`; this task does not depend on it.
- Downstream: 4a (`SkillTreeView`) calls this class. It applies `glTranslatef/glScalef` from the transform and repositions UI children itself, because UI children do not follow GL transforms. It also uses `hitTest` and the zoom/pan API for wheel zoom and drag pan.
- Convention: `PositionAPI.getY()` is the bottom edge and y grows up. Layout space is bottom-up, so tier 0 has the lowest y.

## Requirements
Add `skills_overhaul.ui.layout.TreeLayout` (new package) with these capabilities. Exact method names are yours; keep them small and document them.

- Inputs: `List<TreeDef>` (one tab; one TreeDef = one column group), viewport width and height, node-size bounds (min/max node px). Reuse `PathTreeBuilder`'s spacing rules, but as instance state.
- Geometry, taken from the code, not the April spec:
  - Each TreeDef is one column group. Node x = slot 0..4, y = tier 0..5.
  - Tier 0 (T0) and tier 5 (capstone) are single nodes at slot 2, rendered at the column centre. Tiers 1..4 use slots 0..4 (`PathTreeBuilder.emitLaneColumn`: slot x = `colX + innerPad + slot*(node+slotGap)`; the T0/capstone x = `colX + innerPad + (laneContentW - node)/2`).
  - Spacing ratios in `PathTreeBuilder`: slotGap = clamp(0.18*node, 6, 22), laneGap = clamp(0.40*node, 16, 52), innerPad = clamp(0.20*node, 8, 28), tierGap = clamp(0.30*node, 12, 22). Node size auto-scales 44..120 px today. Column width = 5*node + 4*slotGap + 2*innerPad. Adjacent columns are separated by a gap (the old code uses `laneGap + SUBLANE_W + laneGap`; the sublane is dead code, so use a plain inter-column gap of your choosing, consistent with these ratios).
  - `TreeDef` does not enforce the bounds (it only rejects duplicate coordinates); the tree JSON does. Handle node tiers above 5 and slots outside 0..4 defensively (no exception for a lone out-of-range node; document the behaviour).
- Outputs: per-node rect (x, y, w, h) and per-edge endpoints (parent-node centre to child-node centre; edges come from `Node.getReferencedPrereqIds()` or `getPrereqs()`, both existing) in content space, plus content bounds (width, height).
- View state:
  - Default zoom = fit: the whole content fits the viewport.
  - `zoomAt(cx, cy, factor_or_target)`: zoom is clamped to [fit, 2.0]; the content point under screen point (cx, cy) stays fixed within 0.5 px.
  - Pan is clamped so the content cannot leave the viewport entirely; at fit zoom pan is zero (pan offset = 0, content centred or aligned consistently).
  - `contentToScreen` and `screenToContent` are inverses within 0.01 px at several zoom/pan states.
  - `hitTest(screenX, screenY)` returns the node id whose rect contains the point (via `screenToContent`), or null between nodes.
  - A `resize(viewportW, viewportH)` (or equivalent) refits and re-clamps zoom and pan.
- No `Global`, no GL, no static mutable fields. Static constants and pure static helpers are fine.

## Success criteria
- [ ] TreeLayoutTest passes: for the 5 commander TreeDefs (artificer, collective, commandant, corsair, zealot; 21-22 nodes each, slots 0..4, tiers 0..5) in a 1000x700 and a 600x400 viewport, no two node rects overlap and every node lies inside the content bounds; tier 0 is lowest in y (bottom-up)
- [ ] Default zoom fits the whole content in the viewport; zoom is clamped to [fit, 2.0]; zoomAt(cx, cy) keeps the content point under (cx, cy) fixed within 0.5 px
- [ ] Pan is clamped so content cannot leave the viewport entirely; at fit zoom pan is zero
- [ ] screenToContent(contentToScreen(p)) == p within 0.01 px at several zoom/pan states, and hitTest returns the node id at a node centre and null between nodes
- [ ] TreeLayout uses no Global, GL or static mutable fields; ./build.sh test passes

## Files to modify
Verified with Glob: neither exists yet and neither does the `layout` directory, so create both.

| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/layout/TreeLayout.java` | create | the layout, zoom and pan class |
| `test/skills_overhaul/ui/layout/TreeLayoutTest.java` | create | headless test with `main()` and `assert` |

## Deliverables
| Artifact | Path | Done when |
|---|---|---|
| TreeLayout headless test | `test/skills_overhaul/ui/layout/TreeLayoutTest.java` | runs under ./build.sh test covering fit, zoom clamp, zoom-about-cursor, pan clamp and round-trip transform |

## Implementation guidance
- Pattern to follow (test): `test/skills_overhaul/tree/TreeLoaderRealJSONTest.java` - a `main()` using plain `assert`, ending with `System.out.println("ALL PASSED")`. Copy its `findModRoot()` (walks up from cwd to `mod_info.json`, because `./build.sh test` runs from `jars/classes`) and its loader: `TreeLoader.fromJSON(new JSONObject(new JSONTokener(new FileReader(path))))`. Package `skills_overhaul.ui.layout`. Second style reference: `test/skills_overhaul/tree/PrereqValidatorTest.java`.
- Pattern to follow (spacing rules): `src/skills_overhaul/ui/PathTreeBuilder.java` `configureForViewport(List<TreeDef>,...)` (~line 74), `resetLayoutDefaults`, `totalContentWidth/Height`, `emitLaneColumn` (~line 560-610). Port the ratios as instance state; do not call or modify `PathTreeBuilder` (it still serves the old UI until 4a).
- Test fixture: load the five real files `data/config/skills_overhaul/trees/commander_{artificer,collective,commandant,corsair,zealot}.json` (each is `{"tree":..., "nodes":[...]}`, T0 at (2,0), capstone at (2,5); 21 nodes, collective 22). The widest real tab is the fit case. Lay out all five TreeDefs as one tab as well as each singly.
- Overlap test must use rects from the layout; assert content bounds contain every rect and tier-0 rect y < tier-5 rect y.
- Use `PanelCoords` conventions from 1a: bottom-up, `localY = panelH - yTop - h` for top-relative inputs. The layout itself stays bottom-up and takes no top-down input.
- Zoom range: fit to 2.0. There is no vanilla helper for 2D pan/zoom. Zoom-about-cursor: keep `screenToContent(cx,cy)` invariant across the zoom, then clamp pan.
- Use `float` or `double` consistently; the tolerances (0.5 px, 0.01 px) are easy at double precision, so prefer `double` internally if float rounding at zoom 2.0 on 1000 px content threatens 0.01.
- Java 8 source level: no `var`, no records, no `List.of`.
- Research: `research/external/findings.md` Q7 (zoom has no in-workspace precedent; manual `glTranslatef/glScalef` with inverted hit tests); `research/internal/findings.md` Q1 and Q8 (layout assumes tiers 0..5 and 5 slots in tiers 1-4; static mutable metrics are the thing to avoid).
- Edge cases: empty TreeDef list (no throw; zero-size content, hitTest null); viewport smaller than minimum node bounds (fit zoom drops below 1.0; node-size bounds stay honoured and zoom does the rest, or document otherwise); zoom request below fit or above 2.0 clamps; zero or negative viewport size must not divide by zero.

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08):
- `./build.sh` builds the jar; `./build.sh test` runs every `*Test` class it finds (77 at the time) with `java -ea`; there is no JUnit, so a new `*Test` with `main()` self-registers.
- Campaign listeners: `addListener(l, true)` means transient (not relevant to this task, noted for 4a/5x).

## Testing requirements
Run `./build.sh test` from the repo root (it compiles src and test, then runs each `*Test.class` `main()` with `-ea`, Java 8). Also run `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`. Add `TreeLayoutTest` covering:
- Overlap and bounds, for the five commander TreeDefs, in 1000x700 and 600x400: no two node rects overlap; every rect lies inside the content bounds; tier 0 is lowest in y.
- Fit: default zoom fits all content in the viewport at both sizes; zoom clamp at both ends (below fit, above 2.0).
- zoomAt: the content point under (cx, cy) moves less than 0.5 px, for several cursors including a corner.
- Pan: clamp keeps content from leaving the viewport entirely; zero pan at fit zoom.
- Round trip: `screenToContent(contentToScreen(p))` within 0.01 px at several zoom/pan states.
- Hit test: node id at a node centre; null at a point between nodes; correct after zoom and pan.
- Empty input does not throw.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar. `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: do not commit that file (restore it with `git checkout -- jars/SkillsOverhaul.jar` before committing).
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (this task should not touch them).
- Also do not touch: `src/skills_overhaul/tree/Node.java`, `TreeDef.java`, `data/config/skills_overhaul/trees/**`, `PathTreeBuilder.java` and `CharacterTabPanelPlugin.java` (wiring is 4a).
- Out of scope: the view, tooltips, hosts, model; the April spec's fixed 48x48 nodes, lanes, hybrids and merge nodes; palette; GL drawing; input handling.
- Do not commit unless the caller asks.

## Execution
- Branch: `worktree/skills-panel-restructure-3a`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
