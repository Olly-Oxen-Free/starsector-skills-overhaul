# Task brief: 3b Node tooltip content and TooltipCreator adapter

PRD: skills-panel-restructure | Wave: 3 | Complexity: 3/10 | Model: sonnet | Priority: must
Agent: implementer

## Objective
Add a pure, headless builder that turns a `Node` plus its `SkillTreeModel` state into tooltip content (name, cost, description and effect lines, per-clause prereq met/unmet, blocked reason), and a thin `TooltipCreator` adapter that renders that content into a `TooltipMakerAPI`. Increment 4 (task 4a) attaches it to node components with `addTooltipTo`; this task does not attach it.

## Context
The Skills Overhaul character-tab panel is being restructured (PRD skills-panel-restructure). This task fulfils R6: native tooltips via `addTooltipTo` showing name, cost, effects and prereq met/unmet. Today the hover tooltip is hand-rolled in `CharacterTabPanelPlugin.updateTooltip()` (`src/skills_overhaul/ui/CharacterTabPanelPlugin.java:~280-345`): it shows title, desc and one status line only, and uses `PathTreeBuilder.COLOR_*` constants. 4a will delete that code and `PathTreeBuilder`, which is why the palette is copied into a new `UiColors` class here.
- Builds on 2a (`SkillTreeModel`, `NodeStatus` in `src/skills_overhaul/ui/model/`: per-node OWNED / PENDING / UNLOCKABLE / BLOCKED(reason), reasons PREREQ, TIER, BUDGET(short n)). Those files do not exist in this worktree until 2a merges; read the merged `NodeStatus.java` and `SkillTreeModel.java` for the exact API before coding and consume them as they are.
- Downstream: 4a (`SkillTreeView`) calls `addTooltipTo(new NodeTooltip(...), nodeComponent, TooltipLocation.<x>)`.

## Requirements
- `NodeTooltipContent` (pure Java, package `skills_overhaul.ui.view`): given a `Node`, the node's status from the model, and the set of owned-or-pending node ids (for clause evaluation), produce a content object of plain data (lines with text and a color or style token). It contains:
  - name (fallback to id when name is null);
  - cost line, shown for every status except OWNED (an OWNED node shows no cost line);
  - description line when `getDesc()` is non-empty;
  - one line per effect in `getEffects()`: use the effect's `description` field when present (`optString("description", "")` non-empty), else its `type`;
  - one line per prereq clause, each marked met or unmet via `Prereq.Clause.satisfied(ownedIds)`. Describe `All` (all of ...), `Any` (any of ...), `AtLeast` (at least N of ...) using `getIds()`/`getN()`; show node names rather than ids only if a name lookup is trivially available from the model, otherwise ids are acceptable;
  - for BLOCKED status, a reason line stating why: prereq not met, tier gate (need a node in the previous tier), or "short by N points" for BUDGET.
- `NodeTooltip implements TooltipCreator` (`com.fs.starfarer.api.ui.TooltipCreator`): `isTooltipExpandable` returns false, `getTooltipWidth` returns an explicit constant (suggest 300f; must not be the 500 default), `createTooltip(TooltipMakerAPI, boolean, Object)` renders the content lines with `addPara`/`addTitle` using `UiColors`. Keep it thin; no logic beyond rendering. It takes the content (or a supplier of it) in its constructor; if tooltip text depends on live state, 4a will use the `recreateEveryFrame` overload, so the creator must be cheap to re-run.
- `UiColors`: `public final class` with `public static final java.awt.Color` constants copied by value from `PathTreeBuilder` (lines 57-61): OWNED `(80,200,140)`, PENDING `(230,180,70)`, UNLOCKABLE `(70,150,220)`, LOCKED `(80,90,105)`, CAPSTONE `(220,140,220)`. Do not edit `PathTreeBuilder`.

## Success criteria
- [ ] NodeTooltipContentTest passes: content for an unlockable node lists name, cost and each prereq clause marked met or unmet
- [ ] A BLOCKED node's content states its reason (prereq, tier, or short by N points); an OWNED node shows no cost line
- [ ] NodeTooltip implements TooltipCreator with an explicit getTooltipWidth (not the 500 default) and isTooltipExpandable false
- [ ] NodeTooltipContent has no Global, GL or TooltipMakerAPI dependency; ./build.sh test passes

## Files to modify
| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/view/NodeTooltipContent.java` | create | pure content builder |
| `src/skills_overhaul/ui/view/NodeTooltip.java` | create | TooltipCreator adapter rendering the content |
| `src/skills_overhaul/ui/view/UiColors.java` | create | palette copied from PathTreeBuilder constants |
| `test/skills_overhaul/ui/view/NodeTooltipContentTest.java` | create | headless test of the builder |

(The `ui/view` directories do not exist yet; create them. Verified with Glob at brief time.)

## Deliverables
None beyond the files above.

## Implementation guidance
- Pattern to follow: `test/skills_overhaul/tree/PrereqClauseTest.java:1-40` - test style: a public class with `public static void main(String[] args) throws Exception`, plain `assert cond : "msg"`, no framework. Also see `test/skills_overhaul/level/GameSpendSnapshotTest.java` for a second example. `./build.sh test` compiles with `-source 8 -target 8` (Java 8: no `var`, no `List.of`, no `String.isBlank`) and runs every `*Test` class's `main()`; `assert` works because the runner enables assertions. Check how `build.sh` invokes `java` (around line 40+) if unsure.
- API to consume: `skills_overhaul.tree.Node` (`getName`, `getDesc`, `getCost`, `getClauses`, `getEffects()` returning `List<org.json.JSONObject>`, `isCapstone()`, `getY()`), and `skills_overhaul.tree.Prereq` (`Prereq.All|Any|AtLeast`, each with `getIds()`; `AtLeast.getN()`; `Clause.satisfied(Set<String>)`). Build test nodes directly with `new Node(id, name, desc, x, y, cost, clauses, effects, icon)` and `Prereq.parseClause(new JSONObject("{...}"))` as in PrereqClauseTest.
- Real effect JSON shape in `data/config/skills_overhaul/trees/*.json` is `{ "type": "SO_collective_magnetic_shielding" }`; most effects have no `description`, so the type fallback is the common case.
- TooltipCreator API (`TooltipMakerAPI.java:59-63`): `boolean isTooltipExpandable(Object)`, `float getTooltipWidth(Object)`, `void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam)`. `BaseTooltipCreator` defaults to width 500 and non-expandable; implement the interface directly or extend it but override width explicitly.
- Research: `addTooltipTo` is called on the TooltipMakerAPI that owns the target component, not on the panel; 4a handles that. Tooltip width is fixed by `getTooltipWidth`.
- Keep `NodeTooltipContent` free of `Global`, `GL11`, `TooltipMakerAPI`, `MemoryAPI`. Importing `java.awt.Color` is fine. Do not import `PathTreeBuilder` (4a deletes it).
- Edge cases: null or empty name/desc; node with no clauses (no prereq lines); node with no effects; `AtLeast` with n greater than the id count; effect JSON lacking both description and type (skip or print a neutral placeholder, but do not throw); BUDGET reason with n of 1 ("short by 1 point") versus more; PENDING nodes still show cost.

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08):
- `./build.sh` builds the jar; `./build.sh test` runs 77 test classes (the count predates the audit and this PRD).
- Status: 2026-10-02 audit fixes are compile/unit-test verified only; in-game checks are pending.
- Listeners registered with `addListener(l, true)` are transient (not saved).

## Testing requirements
Run `./build.sh test` (targeted tests via `validate.sh --changed` also fine). Add `NodeTooltipContentTest` covering at least: an unlockable node with a mix of met and unmet clauses (`All`, `Any`, `AtLeast`) lists name, cost and each clause with the right met/unmet marker; a BLOCKED node for each reason (PREREQ, TIER, BUDGET short by N) states its reason; an OWNED node has no cost line; effect line uses `description` when present and `type` otherwise; null/empty desc yields no desc line. A reflection or source check that `NodeTooltip.getTooltipWidth(null) != 500f` and `isTooltipExpandable(null) == false` is acceptable if `NodeTooltip` can be instantiated headlessly (the engine class `TooltipCreator` is on the classpath via `starfarer.api.jar`); do not call `createTooltip`.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar by hand.
- Commit: commit your work on the task branch per the implementer contract (conventional subject, files staged by explicit path). `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: never stage or commit it; run `git checkout -- jars/SkillsOverhaul.jar` before each commit.
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (this task should touch neither).
- Out of scope: attaching tooltips to nodes or any `CharacterTabPanelPlugin` change (4a); editing or deleting `PathTreeBuilder` (4a); changing `SkillTreeModel`/`NodeStatus` (2a); layout, zoom or pan; in-game verification (the project has no compiler beyond `./build.sh`, tooltips are checked in-game in 4a).

## Execution
- Branch: `worktree/skills-panel-restructure-3b`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
- Gaming guard: run `~/.claude/hooks/gaming-check.sh` before the first build or test and before each later build; exit 1 means the owner is gaming: stop, commit WIP, and report `paused: gaming` instead of retrying.
