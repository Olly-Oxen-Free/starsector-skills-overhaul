# Skills panel increment 6 + raid zeal wiring

## Context
The panel restructure (PRD 001, PR #9) left increment 6 open. It also left one audited skill gap: Heavy Detachments never grants its raid zeal stack. The user asked to continue through all remaining work with no further gates. Admin trees, Abyss, Tech Cascade and the forge device visuals stay out of scope.

## Goal
Close four gaps:
1. **One malformed tree JSON stops the whole tab from loading.** `TreeLoader.loadAllForTab` has no per-tree catch. Spec §5 asks for a "Failed to load <tree>" label instead.
2. **Activating a third T0 silently displaces `active.get(0)`.** This is the `TODO(active-basics)` in `SkillTreeView.toggleActiveSlot`. The player should choose which one to replace.
3. **The respec button always says "1 SP".** The first respec per tree is free. A paid respec with no story points looks enabled and then does nothing.
4. **`RaidZealListener.onRaid` has no caller, so Heavy Detachments' raid zeal never fires.**

Work happens in one worktree, branched from `feature/skills-panel-restructure`. The PR targets that branch, so it rides along with PR #9.

## Tasks

### 1. Per-tree load isolation and the failed-tree label
**Files:** `src/skills_overhaul/tree/TreeLoader.java`, `tree/TreeRegistry.java`, `ui/view/SkillTreeView.java`, plus a new test with a malformed JSON fixture.

**Changes:**
- Wrap each tree in `loadAllForTab` in its own try/catch.
- On a failure, log a WARN and record the tree key in `TreeRegistry.getFailed(Tree)`, which returns a `List<String>`.
- When a node id duplicates one in an earlier tree, mark the later tree failed instead of throwing for the whole registry.
- `SkillTreeView` draws one "Failed to load <tree>" header label per failed key, in place of that tree's column.

**Done when:**
- A tab containing one malformed tree still loads its other trees.
- `getFailed` lists the bad key.
- The view code renders the label.
- `TreeRegistry.load()` no longer throws for a single bad tree.
- A headless test proves both the malformed-tree case and the duplicate-id case.

### 2. Choosing the T0 to displace, and the real respec cost
**Files:** `ui/view/SkillTreeView.java`, `level/RespecService.java`, plus tests.

**Displacement:**
- Clicking an owned, inactive T0 while both slots are full enters a "replace" mode.
- In replace mode:
  - The two active T0 nodes pulse.
  - A status line reads "Choose an active basic to replace (Esc to cancel)".
  - Clicking one of the two active T0s calls `ActiveLaneService.swap(mem, chosen, clicked)` plus `reapplyForActiveChange` for both nodes.
  - Esc, or any other click, cancels.
- Combat lock: keep the `canSwapLive()` check.
- The auto-displace of `active.get(0)` and its TODO are removed.

**Respec:**
- Add pure `RespecService.costFor(Map snapshot, Tree tree)`, returning 0 or 1 SP from the `respec_used` key. `respecTree` uses it too.
- The button label shows "Respec (free)" or "Respec (1 SP)" for the current tab's tree.
- When a paid respec cannot be afforded:
  - The button is disabled.
  - Its tooltip says "Needs 1 story point".

**Done when:**
- The auto-displace is gone.
- Replace mode swaps exactly the chosen node.
- The `costFor` test covers both the free and the paid case, and `respecTree` charges what `costFor` reports.
- The label matches `costFor`.

### 3. Raid zeal wiring (bug fix)
**Files:** `effect/handlers/RaidZealListener.java`, `SkillsOverhaulModPlugin.java`, plus a test.

**Changes:**
- `RaidZealListener` implements `ColonyPlayerHostileActListener`.
- Both raid callbacks call one zeal grant:
  - `reportRaidForValuablesFinishedBeforeCargoShown`
  - `reportRaidToDisruptFinished`
- The grant is +1 stack, gated on `SOHeavyDetachmentsHandler.K_PURCHASED`.
- The two bombardment callbacks do nothing. Heavy Detachments is a ground-raid node.
- Extract the gate and grant into a static helper that takes `Map`/`MemoryAPI`, so it can be tested headless.
- Register the listener with `getListenerManager().addListener(l, true)`, guarded by `hasListenerOfClass`. This replaces `addTransientListener`, because `BaseCampaignEventListener` never delivered raids.

**Done when:**
- The regression test fails on the old code: no stack is granted after a raid with the node purchased.
- The test passes after the fix, and grants nothing when the node is unowned.
- No bombardment path grants zeal.

## Test plan
- **Existing tests stay green under `./build.sh test`:** `ActiveLaneServiceTest`, `RespecServiceTest`, `ZealServiceTest`, `TreeLoader*Test`.
- **New tests:**
  - `TreeLoaderFailureTest`: malformed tree and duplicate id.
  - A `costFor` test, inside `RespecServiceTest`.
  - `RaidZealTest`: the regression test for the bug fix.
- **Replace mode is UI-only and is checked in-game:**
  1. With 2 T0s active, click a third. Both active T0s should pulse.
  2. Click one of them. Only that one should be replaced.

## Deliverables
| Deliverable | Path | Check |
|---|---|---|
| Tree load isolation + label | `tree/TreeLoader.java`, `tree/TreeRegistry.java`, `ui/view/SkillTreeView.java` | `TreeLoaderFailureTest` passes |
| T0 replace mode | `ui/view/SkillTreeView.java` | code review; in-game step above |
| Respec cost | `level/RespecService.java`, `ui/view/SkillTreeView.java` | `RespecServiceTest` passes |
| Raid zeal | `effect/handlers/RaidZealListener.java`, `SkillsOverhaulModPlugin.java` | `RaidZealTest` fails before the fix, passes after |
| One PR → `feature/skills-panel-restructure` | GitHub | build and tests green |

## Execution
Tasks run in this order: 1, then 2, then 3. Each runs on a fresh sonnet implementer followed by a code-reviewer, all in one worktree branched from `feature/skills-panel-restructure`. Then validate and open the PR. Never commit `jars/SkillsOverhaul.jar`.
