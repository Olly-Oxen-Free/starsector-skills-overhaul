# Task brief: 2a SkillTreeModel, reason-returning PrereqValidator and all-or-nothing commit

PRD: skills-panel-restructure | Wave: 2 | Complexity: 6/10 | Model: opus | Priority: must
Agent: implementer

## Objective
Build the headless (no Global, MemoryAPI or GL) logic layer for the skills panel: a `SkillTreeModel` that classifies every node as OWNED / PENDING / UNLOCKABLE / BLOCKED(reason), a reason-returning `PrereqValidator` check that includes the tier gate, a `PurchasePlan` dry run, and `GameSpend.commit` that buys a whole pending batch or nothing. Increment 4 later switches the view to these; the old UI wiring stays working until then.

## Context
Skills Overhaul is a Starsector mod (Java 8, no game launch possible in this task). The character-tab panel is a 1300-line `CharacterTabPanelPlugin` that fuses state, drawing and input, commits purchases node by node (a failed confirm leaves earlier nodes bought), duplicates the budget check in UI code, and holds the tier gate as private UI code. This task fulfils PRD requirements R3 (model with reasons, tier gate in `PrereqValidator`) and R4 (all-or-nothing confirm; the red flash itself is view work in 4a, this task only returns the rejection/failure data).
- Builds on 1a (coordinate helper and dead-code purge in the UI; no overlap with these files).
- Consumed by 3b (tooltip content reads model state and reasons), 4a (`SkillTreeView` drives everything from the model, `GameSpend.commit` and flashes the node the failure names).

## Requirements
Add a headless `SkillTreeModel` that maps a `Map<String,Object>` snapshot plus a tab's `List<TreeDef>` and a pending set to per-node OWNED / PENDING / UNLOCKABLE / BLOCKED(reason), with pending add/remove and a rejection result (for the red flash). Add a reason-returning `PrereqValidator` method that includes the tier gate. Widen the snapshot to the whole tab. Add `GameSpend.commit(Tree, Collection<String>)` that dry-runs the batch before writing anything.

Concrete design points:
- `NodeStatus`: states OWNED, PENDING, UNLOCKABLE, BLOCKED; BLOCKED carries a reason enum PREREQ, TIER, BUDGET plus, for BUDGET, the short count n (from `SpendService` reason text `"not enough points (short N)"`, or compute n from the failed `Result`/remaining).
- Reason-returning `PrereqValidator` method (name your choice, e.g. `check(def, mem, pending, nodeId)` returning a small result with ok / PREREQ / TIER): tier gate rule is `y > 0` needs an owned or pending node in tier `y-1` of the same TreeDef (copy semantics from `CharacterTabPanelPlugin.hasAnyInPreviousTier`). Prereq failure and tier failure must be distinguishable. Existing `canPurchase` and `canPurchaseWithPending` keep prereq-only semantics and signatures.
- Budget: use `SpendService.spend` on a copy of the snapshot (pool order tree, tree-locked migration, unallocated if unlocked or locked here, free universal). Do NOT reuse the UI's four-pool `totalPts` / `sumPendingCost` sum in the node-click handler of `CharacterTabPanelPlugin`. Budget for a pending set is cumulative: evaluating a new node means dry-running the existing pending set in topo order and then the new node.
- Model pending API: `add(nodeId)` returns a rejection (reason + node id) and leaves the pending set unchanged when the node is unaffordable or tier-blocked (also PREREQ-blocked); `remove(nodeId)` removes the node plus every pending dependent: re-check each remaining pending node through the model with the new reason-returning `PrereqValidator` check (All/Any/AtLeast clauses AND the tier gate) against owned + remaining pending, and drop any that is no longer ok, repeating until stable; not the flat `Node.getPrereqs()` list (the old `CharacterTabPanelPlugin.removePendingWithDependents` is the bug to not copy).
- `PurchasePlan`: pure class in `skills_overhaul.level`. Input: snapshot map, `Tree`, per-TreeDef pending sets (or equivalent). Output: success with nodes in topological order, or failure naming the offending node id and reason. Sort per TreeDef with `PendingPurchaseTopoSort.sort(def, defPending)` and concatenate per def, as `CharacterTabPanelPlugin.commitPending` does. Dry run = sequential `SpendService.spend` on ONE `HashMap` copy of the input snapshot; the input map must be unchanged afterwards. Also validate prereq/tier per node during the dry run with the new reason method (effective owned set grows as nodes are applied).
- `GameSpend`:
  - Widen the (private) `snapshot` to all TreeDefs of the tab via `TreeRegistry.getTrees(tab)` (purchased flags for every node of every def). Keep `GameSpend.purchasedKeys(TreeDef)` package-private and working (`GameSpendSnapshotTest` uses it).
  - `commit(Tree, Collection<String>)`: build one whole-tab snapshot from `Global.getSector().getMemoryWithoutUpdate()`, run the `PurchasePlan` dry run, write nothing and return failure on a dry-run failure, and play NO sound. On success perform the existing per-node write path for each node in order: `SpendService.spend` on the real snapshot, `RespecService.recordPurchase(m, nodeId, r.fromTreeLocked, r.fromFreeUni, totalMig)`, `writeBack`, `ActiveLaneService.onPurchased(mem, nodeId)` when `n.getY() == 0`, `NodeEffectApplier.apply(n)`. Catch a throw from `NodeEffectApplier.apply` per node, log at ERROR (use the logger style already in `GameSpend`'s neighbours, e.g. `Global.getLogger(GameSpend.class)`), and continue (points already written stay spent).
  - Return type: your choice, but the caller must learn success or the failing node id and reason (reuse `PurchasePlan`'s result type).
  - Leave `GameSpend.purchase` and the old UI wiring (including its `SkillUISounds` call) untouched. Do not move sounds in this task.

## Success criteria
- [ ] SkillTreeModelTest passes: owned, pending, unlockable and blocked states with reasons PREREQ, TIER and BUDGET(short n) computed from a Map snapshot for nodes across two TreeDefs of one tab
- [ ] Adding an unaffordable or tier-blocked node to pending returns a rejection with its reason and leaves the pending set unchanged; removing a pending node also removes pending dependents evaluated with All/Any/AtLeast clauses, not the flat getPrereqs list
- [ ] PurchasePlanTest passes: a batch whose third node is unaffordable returns failure naming that node and the input snapshot map is unchanged; an affordable batch returns nodes in topological order; SpendServiceTest asserts a failed spend leaves the map unchanged
- [ ] PrereqValidator gains a reason-returning check with the tier gate (y>0 needs an owned or pending node in tier y-1 of the same TreeDef); existing canPurchase/canPurchaseWithPending keep prereq-only semantics; existing PrereqValidatorTest assertions are unchanged and still pass, and new assertions in PrereqValidatorTest cover the reason-returning method (PREREQ, TIER, ok)
- [ ] GameSpend.commit(Tree, Collection<String>) builds one whole-tab snapshot, runs the dry run, writes nothing on failure, and plays no sound; GameSpendSnapshotTest and ./build.sh test pass

## Files to modify
| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/model/SkillTreeModel.java` | create | headless node-state model, pending add/remove, rejection result |
| `src/skills_overhaul/ui/model/NodeStatus.java` | create | OWNED/PENDING/UNLOCKABLE/BLOCKED with reason (PREREQ/TIER/BUDGET + short n) |
| `src/skills_overhaul/level/PurchasePlan.java` | create | pure dry run of a topo-ordered batch on one cloned snapshot |
| `src/skills_overhaul/level/GameSpend.java` | modify | whole-tab snapshot, `commit(Tree, Collection<String>)` |
| `src/skills_overhaul/tree/PrereqValidator.java` | modify | add reason-returning check with tier gate |
| `test/skills_overhaul/ui/model/SkillTreeModelTest.java` | create | model states, reasons, add/remove behaviour |
| `test/skills_overhaul/level/PurchasePlanTest.java` | create | all-or-nothing and topo order |
| `test/skills_overhaul/level/SpendServiceTest.java` | modify | add failed-spend-leaves-map-unchanged assertion (append; keep existing asserts) |
| `test/skills_overhaul/tree/PrereqValidatorTest.java` | modify | add assertions for the reason method (append; keep existing asserts) |

## Deliverables
| Artifact | Path | Done when |
|---|---|---|
| Headless model and dry-run tests | `test/skills_overhaul/ui/model/SkillTreeModelTest.java, test/skills_overhaul/level/PurchasePlanTest.java` | both run under ./build.sh test and the all-or-nothing case asserts the snapshot is unchanged after a failed batch |

## Implementation guidance
- Pattern to follow (tests): `test/skills_overhaul/tree/PrereqValidatorTest.java:1-30` builds a TreeDef from an inline JSON string via `TreeLoader.fromJSON(new JSONObject(json))`, keeps a `Map<String,Object> mem`, uses plain `assert`. `test/skills_overhaul/level/SpendServiceTest.java` sets points with `SkillMemory.addPoints(mem, Tree.PILOT, n)`. `GameSpendSnapshotTest` loads real JSON via `findModRoot()`. For the two-TreeDef case, build two inline JSON defs (node ids are globally unique across a tab; `TreeRegistry.java:14-26`). Every test class needs `public static void main`; it self-registers in `./build.sh test`.
- Pattern to follow (impl): `GameSpend.java:23-45` (existing `purchase`: snapshot, validate, spend, recordPurchase, writeBack, onPurchased for y==0, apply) is the per-node write path to reuse in `commit`.
- Key facts (research/internal Q3, `path:Symbol` anchors):
  - `SpendService.spend(Map, Tree, nodeId, cost)` returns `Result{success, fromTreeLocked, fromFreeUni, fromMigrationTo*, reason}`; on failure it returns before any `mem.put` (the `remaining > 0` early return in `SpendService.spend` precedes every put), failure reason `"not enough points (short N)"`. On success it mutates the map incl. `purchasedKey(nodeId)=true` and sets the pool lock.
  - Map keys come from `SkillMemory` builders: `purchasedKey(id)`, `pointsKey(tree)`, `migUniKey(tree)`, `freeUniKey()`, plus `SpendService.POOL_UNALLOC` and `POOL_LOCK_KEY` (a String, only present when set).
  - `PrereqValidator` builds its owned set from `$skills.purchased.<id>` flags in the map (`ownedSet(def, mem)`); the snapshot MUST include those flags for every node of every def, or every prereq-bearing node fails (this was the bug `GameSpendSnapshotTest` pins).
  - Clauses: `Prereq.All`, `Prereq.Any`, `Prereq.AtLeast(n)`; `Prereq.allSatisfied(clauses, ownedSet)` evaluates them; `Node.getClauses()`. Dependents on removal: a pending node stays only if the reason-returning `PrereqValidator` check (clauses and tier gate) is ok against owned + remaining pending; a node left in tier y with no owned or pending node in tier y-1 is dropped even when its clauses still hold.
  - `PendingPurchaseTopoSort.sort(def, pending)` is per TreeDef and throws on a cycle (end of `PendingPurchaseTopoSort.sort`).
  - Tab to TreeDefs: `TreeRegistry.getTrees(Tree tab)`; node to def: `TreeRegistry.findTreeOf(tab, nodeId)`. `Node.getY()`/`getTier()` is the tier, `getX()`/`getSlot()` the slot.
  - Data scan: 184 nodes with y>0, none lacking prereqs, yet the tier gate is not redundant (prereqs can skip tiers).
- Model design: take the snapshot map and `List<TreeDef>` plus `Tree` in the constructor or per call (your choice) so it never touches `Global`. State computation per node: owned flag in snapshot means OWNED; in pending means PENDING; else check prereq, then tier, then budget (cumulative with pending) to give UNLOCKABLE or BLOCKED(reason). Keep the reason check order PREREQ, then TIER, then BUDGET.
- Model, `PurchasePlan` and `NodeStatus` must not import `Global`, `MemoryAPI`, `GL11` or any `com.fs.starfarer` class that needs the engine. `GameSpend` itself may use `Global` (it already does). Keep `level/` free of NEW `skills_overhaul.ui` imports (PRD goal: `level/` stops importing `ui/` after 4a; `GameSpend`'s existing `SkillUISounds` import stays for now). `ui/model` may import `level/` and `tree/`.
- Java 8 only (`-source 8 -target 8`): no `var`, no `List.of`, no records, no `String.repeat`.
- Edge cases: empty pending set (success, no-op, writes nothing); unknown node id (reject, no throw); node already OWNED or already pending (reject, unchanged); two-def tab where a tier-gate check must look only at the node's own TreeDef; pool lock (`POOL_LOCK_KEY` held by another tree blocks the unallocated pool in the dry run); a batch whose first nodes are affordable and the third is not (nothing written, failure names the third).
- Do NOT touch `Node.java`, `TreeDef.java`, `SpendService.java` or tree JSON (PRD non-goals), `mod_info.json`, `build.sh`, `*.jar`.

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08):
- Campaign listeners: `addListener(l, true)` means transient (not saved); `false` saves a new copy every load. `AdminXPTracker` and `SupplyBufferRenderer` were persistent and stacked until fixed.
- Order on load: migration first, then suppression, then reapply, then orphan grants.
- `./build.sh` builds the jar; `./build.sh test` runs 77 test classes.

## Testing requirements
Run `./build.sh test` (project test command; each `*Test` class runs its `main()` with `assert`, Java 8, run with `java -ea`; it compiles src and test against the game jars and stops at the first failing class). Add the tests named here:
- `SkillTreeModelTest` (new, `test/skills_overhaul/ui/model/`): OWNED, PENDING, UNLOCKABLE; BLOCKED with PREREQ, TIER and BUDGET(short n) for nodes across two TreeDefs of one tab; `add` of an unaffordable node and of a tier-blocked node returns a rejection with the reason and leaves pending unchanged; `remove` also removes pending dependents that depend via an `Any` or `AtLeast` clause (a case the flat `getPrereqs` list would get wrong), and a pending tier-y node whose only tier-(y-1) support was the removed node (tier gate).
- `PurchasePlanTest` (new, `test/skills_overhaul/level/`): batch whose third node is unaffordable returns failure naming that node and the input map is equal to a pre-call copy; affordable batch returns nodes in topological order (spanning at least two TreeDefs, concatenated per def).
- `SpendServiceTest` (append): a failed `spend` leaves the map unchanged (compare to a copy).
- `PrereqValidatorTest` (append): reason method returns ok, PREREQ, TIER (y>0 with no owned/pending node in tier y-1 even though prereqs are met); do not alter existing assertions.
- `GameSpend.commit` needs `Global`, so it is not unit-tested headless; keep its logic thin by delegating to `PurchasePlan`. Existing `GameSpendSnapshotTest` must keep passing.
Match style of `test/skills_overhaul/tree/PrereqValidatorTest.java` and `test/skills_overhaul/level/SpendServiceTest.java`.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar by hand.
- Commit: commit your work on the task branch per the implementer contract (conventional subject, files staged by explicit path). `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: never stage or commit it; run `git checkout -- jars/SkillsOverhaul.jar` before each commit.
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (this task should not touch them).
- Also off limits per PRD: `src/skills_overhaul/tree/Node.java`, `TreeDef.java`, `src/skills_overhaul/level/SpendService.java`, `data/config/skills_overhaul/trees/**`, `src/skills_overhaul/zealot/CrusadeNPCStub.java`.
- Out of scope: any view or `CharacterTabPanelPlugin` change (4a does that; do not rewire the old UI to the model), moving the purchase sound, red-flash rendering, `TreeLayout`, tooltips, changing `SpendService` pool order, active-lane displacement changes (increment 6).

## Execution
- Branch: `worktree/skills-panel-restructure-2a`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
- Gaming guard: run `~/.claude/hooks/gaming-check.sh` before the first build or test and before each later build; exit 1 means the owner is gaming: stop, commit WIP, and report `paused: gaming` instead of retrying.
