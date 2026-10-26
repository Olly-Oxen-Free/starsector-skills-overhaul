
## 1a (advisory, PR #1)
- PathTreeBuilder inTL comment still says "same top-down Y convention"; reword (inTL uses a top pad, stored geometry is bottom-up). Hand to 3a.
- portraitY in renderBelow assumes the portrait is the rail's first item at its top edge; verify in M1.
- Pre-existing dead code left for 3a: `pyy` local in processInput, unused `metrics` param of emitLaneColumn, 6-arg refreshEdgesAfterLayout overload, always-false Edge.stippled branch.
- M1 in-game check still pending (user).

## 2a
- Type: api_change | Severity: info | Affected tasks: 3b, 4a | Generalizes: false
  API for the view: `new SkillTreeModel(Tree tab, List<TreeDef> defs, Map snapshot)`; `status(id)` -> `NodeStatus` (state, reason PREREQ/TIER/BUDGET, `shortBy`), null for unknown ids; `statuses()`; `add(id)` -> null or `Rejection{nodeId, status}` (status null = unknown id; OWNED/PENDING status = already taken, `getReason()` null); `remove(id)` -> removed ids (id first). The model holds no snapshot refresh: build a new model after a commit. `GameSpend.commit(tab, ids)` returns `PurchasePlan.Result{success, order, failedNodeId, reason (UNKNOWN/OWNED/CYCLE/PREREQ/TIER/BUDGET), shortBy}`; 4a flashes `failedNodeId` and plays the sound itself on success. The view needs a whole-tab snapshot Map with every purchased flag (GameSpend's snapshot is private; 4a must build an equivalent or expose it).
- Type: discovery | Severity: info | Affected tasks: 4a | Generalizes: false
  `PendingPurchaseTopoSort` orders only by prereq edges, so a tier-y node can precede its tier-(y-1) tier-gate support in a batch. `PurchasePlan` therefore checks each node against owned + the whole batch (not only nodes applied so far); the batch lands as one unit, so this is sound.
- Type: discovery | Severity: info | Affected tasks: none | Generalizes: true
  `SpendService.spend` fills the per-pool take fields of its `Result` before it fails, so the shortfall is `cost - sum(takes)` (`PurchasePlan.shortBy`); no need to parse the "short N" reason text.

## 3b
- Type: blocker | Severity: info | Affected tasks: 4a | Generalizes: false
  3b sources import `skills_overhaul.ui.model.NodeStatus`, which is not on the 3b base (2a unmerged). It compiled and tested with an uncommitted copy of 2a's NodeStatus.java; the branch builds only once 2a is merged first.
- Type: api_change | Severity: info | Affected tasks: 4a | Generalizes: false
  `NodeTooltipContent.build(Node, NodeStatus, Set<String> ownedOrPendingIds)`; `new NodeTooltip(NodeTooltipContent)` or `new NodeTooltip(NodeTooltip.Source)` for recreateEveryFrame. `TooltipCreator` is the nested `TooltipMakerAPI.TooltipCreator`. Prereq lines show ids, not names.

## 3a
- Type: discovery | Severity: info | Affected tasks: 4a | Generalizes: false
  `PanelCoords` (1a) is not on the 3a base (32971ce); it exists only on `origin/worktree/skills-panel-restructure-1a`. TreeLayout does not depend on it (self-contained bottom-up). The 1a advisory items handed to 3a (PathTreeBuilder comment, dead code) were left alone: the brief forbids touching PathTreeBuilder.
- Type: api_change | Severity: info | Affected tasks: 4a | Generalizes: false
  `skills_overhaul.ui.layout.TreeLayout(defs, vw, vh[, minNode, maxNode])`. Screen space is viewport-local bottom-up; `screen = offset + zoom * content`, so apply `glTranslatef(vpX + getOffsetX(), vpY + getOffsetY(), 0); glScalef(getZoom(), getZoom(), 1)`. Wheel: `zoomBy(cx, cy, 1.1)`; drag: `panBy(dx, dy)`; `hitTest(sx, sy)`; `resize(w, h)`; `resetView()`. Geometry: `getNodes()` (NodeRect id/treeIndex/tier/slot/rect), `getEdges()`, `getColumns()` (header band = top `getHeaderHeight()`). Node size is chosen per viewport in [44, 120]; content coordinates change on `resize`, so reposition UI children after it.
- Type: discovery | Severity: info | Affected tasks: 4a | Generalizes: true
  Zoom-about-cursor conflicts with a strict "content covers the viewport" pan clamp: on an axis where the content is narrower than the viewport (always the slack axis at fit), the strict clamp recentres and the cursor point drifts. TreeLayout clamps per axis to `|pan| <= min((v/2)(zoom/fit - 1), (zoom*c + v)/2 - min(zoom*c, v)/4)`: zero at fit, exact for any zoom-in about a point inside the viewport, and the content never fully leaves. Zoom-out shrinks the range and recentres.

## 4a
- Type: discovery | Severity: warning | Affected tasks: 5a, 5b, 5c, all later waves | Generalizes: true
  `worktree.sh add --base feature/skills-panel-restructure` cut the 4a worktree from the stale LOCAL feature branch (32971ce); waves 1-3 were merged only on GitHub. Fixed with `git pull --ff-only origin feature/skills-panel-restructure` inside the worktree (`git merge` is refused by the guard there). Fetch, or fast-forward the local feature branch, before creating wave-5 worktrees.
- Type: api_change | Severity: info | Affected tasks: 5a, 5b, 5c | Generalizes: false
  `new SkillTreeView(w, h)` creates its own panel via `Global.getSettings().createCustom` and builds all children in the constructor; hosts mount `view.getPanel()` and must call `view.discard()` (clears pending) before removing it. `GameSpend.snapshot(Tree)` is now public (whole-tab snapshot for `SkillTreeModel`).
- Type: discovery | Severity: info | Affected tasks: M4 | Generalizes: false
  Untested in game: tooltips are attached with `rail.addTooltipTo(tc, anchor, RIGHT, true)` where `rail` is the left-rail TooltipMakerAPI and `anchor` is a sibling CustomPanelAPI, not a child of the rail; anchors outside the viewport are parked at (-10000, -10000) and partly visible nodes get a clipped anchor. Drag detection uses `Mouse.isButtonDown(0)` to drop a press whose LMB-up was missed.
- Type: discovery | Severity: info | Affected tasks: none | Generalizes: false
  `ui/NodeIconPanelPlugin.java` has no users after this task (was already unused by the deleted classes' callers); `ConnectionLineDrawer.Edge.stippled` is always false. Left in place. The old column headers showed the lane name, which is always "default" (`Node.getLane()`); the view shows the prettified tree id instead (e.g. GUNNERY).
- Type: discovery | Severity: info | Affected tasks: none | Generalizes: true
  `scripts/symbol.sh outline` crashes (TypeError in brace_outline, symbol.py:86) on `SkillTreeView.java`, likely on the anonymous `new NodeTooltip.Source() { ... }` class; per-method greps fell back to line ranges.

## 2a (advisory, PR #2)
- SkillTreeModel.status: budget(plan.shortBy + cost) approximation when the existing pending set fails its own dry run; 4a rebuilds the model after each commit.
- statuses() runs one PurchasePlan dry run per node (O(nodes x pending)); cache if called per frame.
- GameSpend.commit not unit-tested (needs Global); confirm all-or-nothing and single sound in-game (M4).

## 3a (advisory, PR #4)
- TreeLayout spacing ratios duplicated between applySpacing and static columnWidth/widthFor/heightFor; derive from one function.
- TreeLayout.topPad assigned, never read.

## 3b (advisory, PR #3)
- NodeTooltip uses Color.RED/WHITE for unmet/blocked/default instead of UiColors.

## 4a (PR #5)
- Reviewer's only blocking item was the M4 owner in-game sign-off; code criteria met, build+tests pass on bdd0692. Lead merged and deferred M4 to the final gate with M1 and M5.
