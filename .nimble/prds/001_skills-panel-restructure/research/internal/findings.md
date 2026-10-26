# Internal findings — skills-panel-restructure

PRD: skills-panel-restructure | Date: 2026-10-26 | Mode: internal
Paths relative to `workspace/Skills Overhaul/` unless prefixed `game/` (= `Starsector-Mods/game`),
`nex4x/` (= `workspace/nexerelin-4x-mod`), `spec/` (= workspace
`docs/superpowers/specs/skills-overhaul/2026-04-19-skills-overhaul-ui-design.md`). Nothing built or run.
(Researcher's output saved by the lead; the subagent could not write files.)

## Q1. UI classes, callers, dead code
### Takeaway
7 classes / 2901 lines. Only external entry is `CharacterTabReplacer`, registered at
`SkillsOverhaulModPlugin.java:74`. `NodeIconPanelPlugin` has zero callers. Merge/sublane code in
`PathTreeBuilder` is unreachable.
### Cited Findings
- Sizes: CharacterTabPanelPlugin 1300, PathTreeBuilder 810, CharacterTabReplacer 328, ReflectionUtils 192, NodeIconPanelPlugin 142, ConnectionLineDrawer 84, SkillUISounds 45.
- Registration: `addTransientScript(new skills_overhaul.ui.CharacterTabReplacer())` in `onGameLoad` (`SkillsOverhaulModPlugin.java:74`).
- Call graph: replacer creates the plugin (`:199`), calls `PathTreeBuilder.computeMetrics` (`:184`), uses `ReflectionUtils` (`:94,97,101,115,124,134,230,267,270`). Plugin calls `PathTreeBuilder.configureForViewport/buildPaths/refreshEdgesAfterLayout/computeMetrics` (`:221,250,292,495,227`), `ConnectionLineDrawer.draw` (`:606`), `SkillUISounds` (`:966,1244`). `PathTreeBuilder` writes the plugin's package-private maps `edges, nodeCenters, nodeLocal, nodePositions, nodeHitById, nodeStates, nodeBorderColors, pendingByTree` (`PathTreeBuilder.java:543,686-690,706-710`).
- Reverse dependency: `level/GameSpend.java:14` imports `ui.SkillUISounds`, plays sound in `purchase` (`:42`); UI also plays a pend sound (`CharacterTabPanelPlugin.java:1244`).
- `Node.getLane()` always `"default"`, `isMerge()` false, `getParentLanes()` empty (`tree/Node.java:42-45`; pinned by `test/.../tree/NodeSlotTest.java:16-18`). Unreachable: `emitSublaneColumn` (`PathTreeBuilder.java:613`), merge branches (`:580,623,650,715,758,770`), `isCrossLane` (`:740`), `SUBLANE_W`, `merge:` label (`:793`). Each TreeDef = one lane, x = slot 0..4, y = tier 0..5 (`TreeDef.java:20-27`).
- Dead: `configureForViewport(TreeDef,…)` (`PathTreeBuilder.java:94-225`; only the List overload is called at `CharacterTabPanelPlugin.java:221`), `resolveTreeOf` (`:784`), `LaneColumn` fields (written `:460`, never read), `LANE_FRAME, LANE_FRAME_DIM, HEADER_BG` (`:64-66`), `bbLevel/bbPoints` (`CharacterTabPanelPlugin.java:475-476`), deprecated 4-arg `ConnectionLineDrawer.draw` + `flipY=true` branch, empty `render()` (`:818`).
- Static mutable layout: ten `public static float` metrics (`PathTreeBuilder.java:41-54`); `SUBLANE_W = node` repeated at 80,145,166,187,215,308+; plugin reads statics at `:238,351,613,837`.
- Debug text: `UI_VERSION="V17-EVENT-HIT-FIRST"` (`:45`) shown at `:484`, logged `:117,122,127`, replacer `:214`; "Free Uni"/"Mig Pool" `:421-422`; "Keys 1-3 switch tabs" `:425`.
- Per-frame in `renderBelow`: `refreshEdgesAfterLayout` (`:495`) rescans memory, rebuilds edges, calls `canPurchaseWithPending` per node+edge (`PathTreeBuilder.java:515-530,696-737`); `ActiveLaneService.getActive` (`:643`); `loadTexture`+`getSprite` per node (`:830-833`).
- `rebuild()` call sites: 130, 968, 974, 983, 1100, 1121, 1162, 1184, 1197, 1245. Scroll wheel rebuilds (`:1121`). Hover change removes/re-adds a TooltipMaker (`:279-341`).
### Inferences
- A SkillTreeModel can replace `NodeState`, `nodeBorderColors`, `pendingByTree`. Builder inputs reduce to TreeDef list + owned set + pending set.
- Removing merge/sublane code changes no live behaviour; keep or update `NodeSlotTest`.
- PRD must decide whether `GameSpend` keeps calling `ui.SkillUISounds`.
### Gaps
- `data/` and `docs/` not grepped for UI class names.

## Q2. Y-coordinate claim
### Takeaway
Confirmed. `PositionAPI.getY()` and `InputEventAPI.getY()` are absolute bottom-origin; `getY` is the
bottom edge. Only `inTL/inBL` pads are measured from the named edge. Panel code adds top-down offsets
to `getY()` and mixes conventions.
### Cited Findings
- Vanilla `ExampleCustomUIPanel` quad `(x,y)`→`(x+w,y+h)` with `y=p.getY()`; hit test `p.getY()..p.getY()+p.getHeight()` (`game/starfarer.api.zip` → `com/fs/starfarer/api/impl/campaign/ExampleCustomUIPanel.java`).
- LunaLib `glRectf(x, y, x+w, y+h)`, `event.y in y..y+height` (`game/mods/LunaLib-2.0.5/src/lunalib/backend/ui/refit/RefitPanelBackgroundPlugin.kt:42,158`; `lunaUI/panel/LunaBaseCustomPanelPlugin.kt:97,126-127`). SiC same at `second_in_command/ui/panels/BackgroundPanelPlugin.kt:36`.
- nex4x: `float y = position.getY(); // screen bottom of the overlay` (`nex4x/src/nex4x/ui/DiplomacyOverlayPlugin.java:61`), hit test `:114`.
- Wrong comment `CharacterTabPanelPlugin.java:511-513`; wrong math at `winTop` `:514`, `pTop` `:547`, `barTop` `:727`, `trackTop` `:769`, scissor `:590-592`. Arithmetic (not measured): window quad lands at bottom-up `[28, h-14]` while `inTL` content assumes `[14, h-28]`, a 14 px offset.
- Node geometry: `emitNode` stores top-down `nodeLocal` (`PathTreeBuilder.java:686-687`); `syncNodeGeometryFromResolvedPositions` overwrites each frame with bottom-up `p.getCenterY()-py0` (`:541-544`). GL icon drawing (`CharacterTabPanelPlugin.java:620-633`) is therefore correct after frame 1.
- `eventToPanelLocalY` = `(screenH - e.getY()) - py` (`:994-1005`); true top-down is `(py+ph) - e.getY()`; equal only when the panel is vertically centred (`inMid()`, `CharacterTabReplacer.java:203`). Tooltip `ty` (`:365`) and wheel region (`:1111-1112`) rely on this. Backup hit-test `pickNodeAtPanelLocal` (`:1008-1018`, used `:1063,1131`) compares top-down `ly` to bottom-up `nodeLocal`: wrong whenever it fires.
- `PositionAPI.containsEvent` (`:1022-1026`) is the main hit path and is correct, so clicks work today.
- `ConnectionLineDrawer.java:40-44` documents top-down too (wrong).
### Inferences
- Use bottom-up panel-local everywhere (`py + localY`); convert `inTL(x, yTop)` as `localY = panelH - yTop - h`; cursor `event.getY() - py`.
- All sites (`:514,547,727,769,590-592`, tooltip background) and `ConnectionLineDrawer` change together. Put conversion helpers in a pure, unit-testable class.
### Gaps
- Not run in-game; 14 px is arithmetic. Bottom-up `getCenterY()` for nested children inferred from nex4x/SiC usage.

## Q3. Purchase state and model APIs
### Takeaway
State lives in campaign memory behind `SkillMemory` key builders and `Map<String,Object>` snapshots.
`SpendService`, `PrereqValidator`, `ActiveLaneService`, `PendingPurchaseTopoSort`, `SkillMemory` all
have Map entry points. Tier gate is UI-only.
### Cited Findings
- Keys (`state/SkillMemory.java:7-17`): `$skills.purchased.<id>`, `$skills.points.<treeKey>`, `$skills.points.universal_free`, `$skills.points.universal_migrated_<treeKey>`, `…_unallocated` (`SpendService.POOL_UNALLOC`), `…_locked_to` (`POOL_LOCK_KEY`, `SpendService.java:10-12`), `$skills.<treeKey>_xp/_level`, `$skills.activeBasics`, `$skills.respec_used.<key>`, `$skills.spend_record.<node>.tl/.fu/.mg`. Map and MemoryAPI overloads (`:20-74`). Node ids globally unique (`TreeRegistry.java:14-26`).
- `SpendService.spend(Map, Tree, nodeId, cost)` → `Result{success, fromTreeLocked, fromFreeUni, fromMigrationTo{Pilot,Commander,Admin}, reason}` (`:14-29`); pool order tree → tree-locked migration → unallocated (if unlocked or locked here) → free universal (`:38-64`); failure reason `"not enough points (short N)"` (`:66-70`).
- No dry-run method, but `spend` returns before mutating on failure (`:66-82`); success mutates incl. `purchasedKey` (`:78`). Dry run = `spend` on a copy; batch = sequential `spend` on one copy in topo order. `SpendServiceTest` does not assert the failed-spend-no-mutation property.
- `GameSpend.snapshot(mem, def)` is private (`GameSpend.java:48`), covers ONE TreeDef; `purchasedKeys(def)` package-private (`:68`).
- `GameSpend.purchase(Tree, id)` (`:23-45`): snapshot → `PrereqValidator.canPurchase` → `spend` → `RespecService.recordPurchase` → `writeBack` → `ActiveLaneService.onPurchased` if y==0 → `NodeEffectApplier.apply` → sound. Writes back per node, so the UI loop (`CharacterTabPanelPlugin.java:1267-1275`) leaves earlier nodes bought on failure.
- UI duplicate budget check (`:1208-1242`) sums four pools vs `sumPendingCost + clicked.getCost()`, ignores integer truncation, only WARNs on failure (`:1239`).
- `PrereqValidator.canPurchase(def,map,id)` / `canPurchaseWithPending(def,map,pending,id)` return boolean, no reason (`tree/PrereqValidator.java:9-27`). Clauses All/Any/AtLeast(n) (`Prereq.java:23-59`).
- Tier gate `hasAnyInPreviousTier` private UI code (`CharacterTabPanelPlugin.java:937-948`), only called `:1225`; `GameSpend.purchase` does not apply it. Only the UI calls `GameSpend.purchase`/`PrereqValidator` (`:1226,1268`; `PathTreeBuilder.java:642,717`).
- `emitNode` colours unlockable by prereqs only (no tier gate, no affordability).
- Data scan: 184 nodes with y>0, none lacking prereqs; does not make the tier gate redundant (prereqs can skip tiers).
- `ActiveLaneService`: `MAX_ACTIVE=2`; `getActive,isActive,add,swap,onPurchased,onUnpurchased`; `canSwapLive()` checks combat engine (`basics/ActiveLaneService.java:17-121`). UI toggles active T0 on owned y==0 click, auto-displacing `active.get(0)` with `TODO(active-basics)` (`:1166-1200`); spec §4.3 says click owned = no-op.
- `removePendingWithDependents` (`:1278-1299`) uses flat `getPrereqs()`, ignoring Any/AtLeast.
- `PendingPurchaseTopoSort.sort(def, pending)` throws on cycle (`tree/PendingPurchaseTopoSort.java:42-44`), per TreeDef; UI concatenates (`:1254-1266`).
- `RespecRunner.run(Tree)` returns null when refused (`level/RespecRunner.java:18`).
### Inferences
- Model input: Map snapshot + `List<TreeDef>`; output OWNED/PENDING/UNLOCKABLE/BLOCKED(reason). Reasons: PREREQ, BUDGET (short count), TIER (new), ALREADY; active-swap combat lock separate.
- All-or-nothing: dry-run topo-ordered batch via `spend` on one cloned whole-tree snapshot (widen `GameSpend.snapshot` to all defs of the tree), then `GameSpend.purchase` per node. Residual risk: `NodeEffectApplier.apply` throwing mid-batch.
- `PrereqValidator` needs a reason-returning API; keep boolean wrappers for `PrereqValidatorTest`.
### Gaps
- `NodeEffectApplier.apply` and `RespecService.respecTree` internals not read.

## Q4. Tests and runner
### Takeaway
77 `*Test.java`, plain `main()` + `assert`, no `ui` dir. Spend, prereq, topo, snapshot, active-lane
covered; tier gate, over-budget feedback, all-or-nothing not.
### Cited Findings
- `build.sh test` (`build.sh:15-47`): wipes `jars/classes`, compiles src+test `-source 8 -target 8` against game jars, runs every `*Test.class` with `java -ea`, exits on first failure. New `*Test` with `main` self-registers.
- Relevant: `level/SpendServiceTest`, `level/GameSpendSnapshotTest` (real JSON via `findModRoot()`, uses package-private `GameSpend.purchasedKeys`), `tree/PrereqValidatorTest`, `PrereqClauseTest`, `PendingPurchaseTopoSortTest`, `GridValidationTest`, `TreeLoader*Test`, `basics/ActiveLaneServiceTest`, `level/RespecServiceTest`, `level/RoundTripTest`, `tree/NodeSlotTest`.
- Map paths run headless; `GL11`, `TooltipMakerAPI`, `Global.getSettings()` do not.
### Inferences
- New headless tests: `SkillTreeModelTest`, `SpendServiceDryRunTest`, all-or-nothing commit test, `TreeLayoutTest` (geometry, zoom/scroll clamps, bottom-up conversion).
### Gaps
- Runner not executed.

## Q5. Reflection
### Takeaway
All UI reflection goes through `ReflectionUtils` (MethodHandle-based near-copy of SiC's), driven only
by `CharacterTabReplacer`. `zealot/CrusadeNPCStub` reflects outside `ui/`. Replacer hard-links
obfuscated `CampaignState` and `AppDriver`.
### Cited Findings
- `ReflectionUtils`: `invoke(name,target)`, `hasMethodOfName`, `getChildrenCopy` (List-field fallback), `getParent` (`ui/ReflectionUtils.java:83,104,125,150`); caches `:78,160-161`.
- Replacer call sites: `getEncounterDialog` `:94`, `getCoreUI` `:97`, `getCore` `:101`, `getCurrentTab` `:115`, `getParent` `:124`, `getChildrenCopy` `:134,230,267`, `hasMethodOfName("canReassign")` `:270` (largest descendant with that method, `:263-293`).
- Obfuscated imports `com.fs.starfarer.campaign.CampaignState`, `com.fs.state.AppDriver` (`CharacterTabReplacer.java:8-9`, used `:81-89`).
- Hiding by mutation: `setSuspendRecompute`, `setSize`, `setLocation(-7000,-7000)` siblings, `(-5000,-5000)` panel fallback (`:166-172,227-247`).
- Non-UI: `Class.forName("exerelin.campaign.DiplomacyManager")` + Method loop (`zealot/CrusadeNPCStub.java:36,46-47`). No other reflection in `src`.
- SiC uses the same calls: `SkillPanelReplacerScript.kt:37-60`, `misc/UIExtensions.kt:8-37`.
### Inferences
- Probe wraps those seven calls + two obfuscated refs; returns `{core, vanillaPanel, vanillaParent}` or a failure reason. Scope "only reflection class" to `ui/`.
- `getChildrenCopy` swallows Throwables → empty list, so failure looks like "not found" and retries silently; probe needs an explicit result type.
### Gaps
- Game-version sensitivity untested (SiC targets 0.98a-RC5, this mod 0.98a-RC8).

## Q6. Host: trigger, mount, dialog
### Takeaway
Replacer polls every frame and installs nothing under SiC. SiC also polls. nex4x proves the
`CoreUITabListener` trigger and its early firing. Dialog and hotkey have in-workspace precedents.
### Cited Findings
- Replacer lifecycle (`CharacterTabReplacer.java:45-78`): runs while paused each frame; returns unless paused + CHARACTER tab; `install()` retried every frame; remount on resolution change (`:59-63`); uninstall on unpause or tab change (`:51-55`).
- SiC branch (`:127-145`): `isModEnabled("second_in_command")` or sibling `SCSkillMenu` → `installed=true; deferredToSiC=true`; no UI.
- Silent no-warning returns: `:122` (no target), `:125` (no parent).
- Mount today: added to `core`, `inMid()`, size `min(1700,0.9W) x min(950,0.88H)` (`:190-211`); siblings parked and restored (`:225-261`).
- SiC mount: `SkillPanelReplacerScript.advance` (`.kt:30-62`) finds panel, `parent.removeComponent(panel)`, `createCustom(parent.w, parent.h, null); parent.addComponent(panel)` (`ui/SCSkillMenuPanel.kt:97-104`). No parking, no listener (transient script, `SCModPlugin.kt:117`). After SiC removes it, no `canReassign` panel remains.
- nex4x: `CoreUITabInjectorListener implements CoreUITabListener`, filters INTEL (`nex4x/src/nex4x/ui/CoreUITabInjectorListener.java:56-71`); registered `addListener(…, true)` guarded by `hasListenerOfClass` (`Nex4xModPlugin.java:359-368`); comment: fires before the switch completes, always defer (`:73-80`); retry script `MAX_RESOLVE=180` (`IntelDiplomacyTabInjector.java:38,98-110`); "no symmetric close hook" (`:127`); statics reset on game load (`:36-40`; `Nex4xModPlugin.java:132`); mounts in existing panel `addComponent`, `bringComponentToTop`, `inTL(0,0)` (`:121-126`).
- API (`game/starfarer.api.zip`): `CoreUITabListener.reportAboutToOpenCoreTab(CoreUITabId, Object)`; `CampaignInputListener.getListenerInputPriority()` + `processCampaignInputPreCore/PreFleetControl/PostCore(List<InputEventAPI>)`; `CustomVisualDialogDelegate.init(CustomPanelAPI, DialogCallbacks)`, `getCustomPanelPlugin()`, `getNoiseAlpha()`, `advance(float)`, `reportDismissed(int)`; `DialogCallbacks.dismissDialog()`; `InteractionDialogAPI.showCustomVisualDialog(float,float,CustomVisualDialogDelegate)`; `CampaignUIAPI.showInteractionDialog(InteractionDialogPlugin, SectorEntityToken)`, `isShowingDialog()`, `getCurrentInteractionDialog()`, `isShowingMenu()`, `getCurrentCoreTab()`; `TooltipMakerAPI.addTooltipTo(TooltipCreator, UIComponentAPI, TooltipLocation)` (line 333), `addTooltipToPrevious(tc, loc[, recreateEveryFrame])`.
- Precedents: `CampaignInputListener` in `game/mods/Console Commands-4.0.9/src/org/lazywizard/console/ConsolePlugins.kt:4,20`; `showCustomVisualDialog(1000f,700f,delegate)` in `game/mods/Interastral Peace Casino-1.6.3/src/data/scripts/casino/interaction/GachaHandler.java:414,500`.
- None of `CoreUITabListener|CampaignInputListener|showCustomVisualDialog|addTooltipTo` used in `src`. Non-persistent listener convention `addListener(l, true)` (`SkillsOverhaulModPlugin.java:69-70`).
### Inferences
- Listener sets a flag/adds a one-shot transient script; mount on next `advance`; cap retries (180).
- Mounting in the parent removes sibling-hiding; size comes from the parent, so layout must fit it — zoom/scroll (increment 3) is a precondition for increment 5.
- No close hook: detect tab-away with a cheap `advance` check while mounted.
- Hotkey listener should skip when `isShowingDialog()` or a core tab is open (inference).
### Gaps
- Not verified that the listener fires for CHARACTER like INTEL; opening a dialog with no entity target unchecked; vanilla parent lifetime on tab flip undocumented.

## Q7. UI spec vs code
### Takeaway
The April spec already requires no partial commits, over-budget red flash, `addTooltipTo` tooltips and
a SiC defer. Code departs in several places.
### Cited Findings
- §4.5 "No partial commits", over-budget "flashes red briefly" vs silent WARN + partial loop (`:1239,1267-1275`). §4.3 step 3 describes the partial loop and needs amending.
- §4.1 tooltip via `addTooltipTo()` with name, cost, effect list, prereq ✓/✗ vs own element (`:279-341`).
- §4.1 colours (teal, slate, `#1a2028/#556`, amber, green hybrid, brown capstone) vs code `OWNED(80,200,140)`, `PENDING(230,180,70)`, `UNLOCKABLE(70,150,220)`, `LOCKED(80,90,105)`, `CAPSTONE(220,140,220)` (`PathTreeBuilder.java:57-62`).
- §4.3 "click owned = no-op" vs active-T0 toggle. Esc consumed only with pending (`:1093-1103`), matches.
- §2.1 tab leave clears pending: happens only because the plugin is discarded (`CharacterTabReplacer.java:299-302`); internal tab switch clears only the left tab (`:979-984`).
- §5/§6 test 8: SiC → skip + INFO log; matches but leaves no UI; DialogHost supersedes.
- §2.2/§4.1 fixed 48x48 nodes, 3 lanes, hybrids at ~1000x700 vs code auto-scaling 44-120 px, 1700x950 cap, no lanes/hybrids. §3.2/§3.3 (`TreeDef.getMergeNodes()`, `MergeNodeTest`) never implemented.
- §2.3 stale file names (`TreeGridBuilder.java` → `PathTreeBuilder`; plugin path).
- Tab hotkeys 1-3 (`:1081-1092`) beyond spec.
### Inferences
- §4.3/§4.5/§4.1 are already-approved behaviour for increments 2 and 4. Code, not spec, is the baseline for increment 3.
### Gaps
- No spec text for DialogHost, zoom, scroll offsets, hotkey.

## Q8. Other inconsistencies
### Cited Findings
- Hover reset by every `rebuild()` (`:149-151`).
- `maybeUninstall()` runs whenever the sector unpauses with the tab open (`CharacterTabReplacer.java:51`); `reflectionWarned` never reset.
- Replacer calls `computeMetrics` before `configureForViewport` (`:182-188`), logging unconfigured sizes.
- Log noise: `processInput is live` (`:1043`), `attach … jar=` (`:114-129`).
- `Node.isCapstone()` = y==5; TreeDef hard-codes t0/t5; layout assumes tiers 0..5, 5 slots in tiers 1-4 (`PathTreeBuilder.java:578-610`).
- Absent: `ui` tests, the four APIs above, reason-returning `PrereqValidator`, public snapshot builder, zoom/vertical scroll (only `scrollXByTree`, `:61`), `TreeLayout`, model class.
### Inferences
- Increments 1-3 land without touching the host; increment 5 forces layout to fit the parent.
- Keep new tests on the Map path (`GameSpend.purchase` cannot run headless).
### Gaps
- `data/config/settings.json` and other `data/config/skills_overhaul/*` not read for UI settings.
