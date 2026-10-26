# Review: skills-panel-restructure (briefs), 2026-10-26

Blocking: 2   Advisory: 11
After apply (2026-10-26): Blocking open: 0   Advisory open: 1 (A11, left optional). See `## Applied`.

Plan-stage (prd) review: replaced; read it with
`git show 289fe4c:.nimble/prds/001_skills-panel-restructure/review/recommendations.md`.

Checks that passed:
- `waves.sh list` exits 0: 1:[1a] 2:[2a] 3:[3a,3b] 4:[4a] 5:[5a,5b] 6:[5c]. Every task's wave is
  above its dependencies' waves.
- No files overlap within a wave. 3a (`ui/layout/*`) and 3b (`ui/view/*`) share no file. 5a and 5b
  share only the new directory `test/skills_overhaul/ui/host/`, which git does not track.
- R1-R10 each map to a brief: R1/R2 1a; R3/R4 2a+4a; R5 3a+4a; R6 3b+4a; R7 4a; R8 5a+5c;
  R9 5b+5c; R10 5c.
- Every `## Deliverables` table matches its `tasks.yaml` entry.
- No brief edits a `never_touch` path. Every brief says not to commit the
  build-rewritten `jars/SkillsOverhaul.jar` (1a:70, 2a:88, 3a:84, 3b:64, 4a:94, 5a:77, 5b:74,
  5c:73). `jars/SkillsOverhaul.jar` is tracked (`git ls-files jars`) and is already modified in
  the main checkout.
- The current-state claims I checked hold. Symbols and approximate lines in
  `CharacterTabPanelPlugin`, `PathTreeBuilder`, `CharacterTabReplacer`, `GameSpend`,
  `SpendService`, `PrereqValidator`, `Prereq`, `Node`, `TreeRegistry` and
  `PendingPurchaseTopoSort` match. So do the API signatures in `starfarer.api.zip`
  (`TooltipCreator`, `addTooltipTo` :333-334, `CustomVisualDialogDelegate`,
  `CampaignInputListener`, `CoreUITabListener`, `hasListenerOfClass`, `hasTransientScript`,
  `addMessage`). The five commander tree files have 21 or 22 nodes in slots 0..4 and tiers 0..5,
  with T0 and the capstone at slot 2. `Config` and `ConfigTest` have the shape 5b describes, and
  `data/config/skills_overhaul.json` is valid JSON. The `build.sh` test runner does what the
  briefs say.

## Blocking

### B1. CharTabHost has no defined way to report to HostRouter, and the two tasks both log WARN for probe failures
- Where: `briefs/5a_skills-panel-restructure.md:13,26` and `briefs/5c_skills-panel-restructure.md:21-24,56,75`.
  The same applies to the `tasks.yaml` criteria for 5a and 5c.
- Actual: 5a requires CharTabHost to log "one WARN per distinct probe failure reason per session"
  (5a:26) and only to "expose results". `CharTabHost` runs itself from `advance`, so it is its own
  caller, and 5a does not define a callback. 5c must then (a) send the 180-frame exhaustion
  report and the SiC result to `HostRouter` and (b) log "exactly one WARN". But
  `CharTabHost.java` is not in 5c's `files_affected` (`tasks.yaml:576-582`), and 5c:75 puts
  changes to `CharTabHost` out of scope. As written, 5c either cannot connect the router or
  breaks its own scope. Probe failures would also log twice: once per distinct reason from 5a,
  and transient early-frame reasons such as NO_CORE would also be logged during mounts that
  succeed. A second WARN would then come from HostRouter. That contradicts the 5c criterion
  "logs one WARN".
- Fix:
  - 5a requirement 7: log each failed attempt at DEBUG only. Add a constructor argument
    `CharTabHost(HostEvents events)`, where `HostEvents` has `onSicDetected()` and
    `onProbeExhausted(String lastReason)`. Call each at most once per arming.
  - 5a success criteria and `tasks.yaml` 5a: add "SiC detection and exhaustion are reported
    through a HostEvents callback passed to the constructor".
  - 5c: implement `HostEvents` in `HostRouter` and pass it when constructing CharTabHost. The
    WARN is logged only in HostRouter.

### B2. Two briefs tell the implementer not to commit, which the workflow needs it to do
- Where: `briefs/4a_skills-panel-restructure.md:98` ("the caller said not to commit in this step")
  and `briefs/5c_skills-panel-restructure.md:76` ("Do not commit; leave changes in the worktree").
  A weaker form is in 1a:70, 3a:88 and 5b:74 ("unless the caller asks").
- Actual: the implementer contract commits on the task branch as step 9
  (`.../plugins/nimble/agents/implementer.md:3,77`). The waves workflow merges task branches.
  If 4a is left uncommitted, waves 5-6 start without `SkillTreeView` (5a:12, 5b:16 expect it
  merged). The jar is the only file that must stay out of a commit.
- Fix: in all five briefs, replace the no-commit line with "Commit your work on the task branch
  per the implementer contract. Never stage `jars/SkillsOverhaul.jar` (restore it with
  `git checkout -- jars/SkillsOverhaul.jar` first)." Delete "the caller said not to commit in
  this step" from 4a.

## Disproof attempts
- Not run for the `briefs` target (prd only). The plan-stage results (all three decisions held)
  are in commit 289fe4c.

## Advisory

### A1. 5b creates the view inside `init`, but the precedents create the plugin before the dialog opens
- Where: `briefs/5b_skills-panel-restructure.md:16`
- Why: both precedents construct the plugin before `showCustomVisualDialog`. `getCustomPanelPlugin`
  returns that existing object, and `init` only attaches the panel:
  `LunaLib-2.0.5/src/lunalib/backend/ui/OpenCustomPanelFromDialog.kt:65-77` and
  `Interastral Peace Casino-1.6.3/.../blackjack/BlackjackDialogDelegate.java:33-42`. If the engine
  calls `getCustomPanelPlugin()` before `init`, which is how the panel is created with a plugin,
  the brief's order returns null and the dialog is blank. This could be blocking. I cannot prove
  the engine's call order from the obfuscated jar, so it is filed as advisory.
- Fix: in 5b requirement 2, construct `SkillTreeView` in the delegate constructor, return it from
  `getCustomPanelPlugin()`, and in `init` only store the callbacks and attach the panel. This is
  the same way `CharacterTabReplacer.java:199-210` does `createCustom` then `attach`.

### A2. 5c should register one CharTabHost instance as both the listener and the script
- Where: `briefs/5c_skills-panel-restructure.md:19`
- Why: 5a:23 makes CharTabHost both the `EveryFrameScript` and the `CoreUITabListener`, and its
  arm-once state lives in that instance. If 5c creates two instances, the listener instance's
  `advance` never runs. The listener trigger then does nothing, and the arm-once-per-open
  behaviour is never exercised. The repo already registers one shared instance:
  `SkillsOverhaulModPlugin.java:81-84` (`privLifecycle` passed to both `addTransientScript` and
  `addTransientListener`).
- Fix: add to 5c requirement 1: "Construct one CharTabHost and pass the same instance to
  `addListener(host, true)` and `addTransientScript(host)`. Check both guards before
  constructing it."

### A3. The 5a probe guidance names only one of three ReflectionUtils calls that swallow errors
- Where: `briefs/5a_skills-panel-restructure.md:16`
- Why: `ReflectionUtils.invoke` catches Throwable, logs a WARN and returns null
  (`ReflectionUtils.java:95-100`). It also logs WARN on "method not found" (:87-91).
  `hasMethodOfName` swallows errors silently (:117). Over 180 retry frames this can produce up to
  180 WARN lines, and REFLECTION_ERROR cannot be told apart from a method that returned null.
- Fix: in 5a requirement 1, add `invoke` and `hasMethodOfName` to the swallow note. Require the
  probe to use a non-logging, throwing variant (a new method in ReflectionUtils is acceptable) so
  it can report REFLECTION_ERROR.

### A4. In 2a, `remove()` re-checks only prereq clauses, not the tier gate
- Where: `briefs/2a_skills-panel-restructure.md:21` and :61 ("a pending node stays only if
  `Prereq.allSatisfied(...)`")
- Why: the tier gate counts pending nodes (`CharacterTabPanelPlugin.java:937-948`), and prereqs
  can skip tiers (2a:62). Removing the only pending node in tier y-1 can leave a tier-y node
  pending while it is tier-blocked. The commit dry run, which checks the tier, then fails and
  flashes red for a batch the model said was valid.
- Fix: the removal loop keeps a pending node only if the new reason-returning `PrereqValidator`
  check returns ok against owned + remaining pending. Add one `SkillTreeModelTest` case for a
  tier-only dependent.

### A5. The manual-deliverable paths name a folder that does not exist
- Where: `tasks.yaml:313,467,588`; `briefs/1a:45`, `4a:57`, `5c:48`
- Why: the paths say `.nimble/prds/skills-panel-restructure/...`, but the folder is
  `.nimble/prds/001_skills-panel-restructure/`. Nothing checks these paths automatically, but
  they lead nowhere.
- Fix: change `skills-panel-restructure/` to `001_skills-panel-restructure/` in all six places.

### A6. 1a puts the UI_VERSION and attach logs in the wrong files
- Where: `briefs/1a_skills-panel-restructure.md:36,38`
- Why: `UI_VERSION` is declared at `CharacterTabPanelPlugin.java:45`, not in PathTreeBuilder. Its
  attach logs, including `jar=`, are at `CharacterTabPanelPlugin.java:117-127`, and a rebuild
  debug log is at :135. PathTreeBuilder contains no `UI_VERSION`. The criterion's grep would
  still catch them.
- Fix: move these items into the CharacterTabPanelPlugin row (`:45`, `:117-135`, `:484`). Keep
  only `:214` in the CharacterTabReplacer row.

### A7. Stale anchors and paths in several briefs
- Where and actual state:
  - 2a:58 says `SpendService.java:~66-70`; the early return is at :55-59.
  - 3a:57 says `configureForViewport(List<TreeDef>,...)` is at ~74; it is at :273. Line 74 is
    `resetLayoutDefaults`. Note that this live overload separates trees by `2*laneGap`.
  - 5c:53 says AdminXPTracker is at :60-65; it is at :69-70.
  - 5a:43 says `test/skills_overhaul/ui/` does not exist; 1a and 2a create it.
  - 5b:51 gives the path `M/LunaLib-2.0.5/...`. The real path is
    `game/mods/LunaLib-2.0.5/src/lunalib/backend/ui/OpenCustomPanelFromDialog.kt`, and
    `LunaBaseCustomPanelPlugin.kt` is under `src/lunalib/lunaUI/panel/`.
- Fix: correct these anchors. Each brief already says to locate code by symbol, so none of these
  blocks execution.

### A8. 5c's "top `## Unreleased`" CHANGELOG section is ambiguous
- Where: `briefs/5c_skills-panel-restructure.md:25`
- Why: `CHANGELOG.md:3` and `:34` are two separate `## Unreleased — ...` sections, and both
  cover other work (defense lane, gunnery).
- Fix: tell 5c to add a new `## Unreleased — Skills panel restructure` section above line 3.

### A9. Some 5c behaviour can only be seen in-game, and M5 does not check it
- Where: 5c criteria 2-3 (`tasks.yaml:572-573`); PRD §8 M5
- Why: the SiC INFO line, the campaign message shown once, and the probe-failure WARN cannot be
  checked headless. M5 does not list them, and the user cannot trigger the probe-failure path.
- Fix: add to M5 and the 5c deliverable: "with SiC enabled, `starsector.log` has one INFO line and
  one campaign message names the hotkey; reload shows it once again". Record the probe-failure
  path as checked by code review only, or cover it through the optional `HostRouterTest` (A11).

### A10. Only two briefs include the gaming-guard step
- Where: 3b:72 and 4a:104 have it; 1a, 2a, 3a, 5a, 5b and 5c do not.
- Why: the hooks and `validate.sh` enforce the guard anyway, so this only makes the briefs
  inconsistent.
- Fix: add the same Execution line to the other six briefs, or remove it from both.

### A11. 5c's optional `HostRouterTest` is not in the plan
- Where: `briefs/5c_skills-panel-restructure.md:69`
- Why: the PRD success metric counts 7 new test classes. An optional eighth test that is not in
  `files_affected` leaves the scope unclear.
- Fix: either list `test/skills_overhaul/ui/host/HostRouterTest.java` in `tasks.yaml` 5c with a
  criterion (SiC plus probe failure shows one message), which also helps A9, or drop the line.

## Applied
- B1 (resolved):
  - `briefs/5a`: requirement 7 now logs failed attempts at DEBUG only. A new requirement 8
    defines `HostEvents { onSicDetected(); onProbeExhausted(String) }` as a `CharTabHost`
    constructor argument, with each called at most once per arming. The Downstream line, the
    success criterion and the file table (`HostEvents.java`) are updated.
  - `briefs/5c`: `HostRouter` implements `HostEvents` and is passed to the `CharTabHost`
    constructor. It is the only place that logs the SiC INFO and the probe-failure WARN.
  - `tasks.yaml` 5a: criterion, `files_affected` (+`HostEvents.java`, +`ReflectionUtils.java`) and
    `agent_context`. `tasks.yaml` 5c: criterion 1 and `agent_context`.
- B2 (resolved), all 8 briefs:
  - Every `## Boundaries` section now has the same `- Commit:` bullet: commit on the task branch
    per the implementer contract, never stage `jars/SkillsOverhaul.jar`, and run
    `git checkout -- jars/SkillsOverhaul.jar` before each commit.
  - All "do not commit" lines are removed. 4a's `.serena/` note is kept as its own bullet.
- A1 (resolved):
  - `briefs/5b` requirement 2: the `SkillTreeView` plugin is created before
    `showCustomVisualDialog`, `getCustomPanelPlugin()` returns it, and `init` only stores the
    callbacks and attaches the view to the panel.
  - `tasks.yaml` 5b `agent_context`: same rule added.
- A2 (resolved):
  - `briefs/5c` requirement 1: one `CharTabHost` instance is registered with both
    `addListener(host, true)` and `addTransientScript(host)`, following the `privLifecycle`
    precedent. Both guards are checked before it is constructed.
  - `tasks.yaml` 5c criterion 1: updated to match.
- A3 (resolved): `briefs/5a` requirement 1 now names `getChildrenCopy`, `invoke`/`getParent` and
  `hasMethodOfName`, and asks for new non-logging, throwing variants in `ReflectionUtils`, leaving
  the existing methods unchanged. `ReflectionUtils.java` is added to 5a's files. There is no
  conflict within wave 5, because 5b does not touch it.
- A4 (resolved):
  - `briefs/2a`: the pending-removal cascade re-checks each remaining pending node with the
    reason-returning check (clauses and tier gate) until the set is stable. The key-facts line and
    the `SkillTreeModelTest` cases now include a tier-only dependent.
  - `tasks.yaml` 2a criterion 2: updated to match.
- A5 (resolved): `skills-panel-restructure/` changed to `001_skills-panel-restructure/` in
  `tasks.yaml` (3 places) and in briefs 1a, 4a and 5c.
- A6 (resolved): in `briefs/1a`, `UI_VERSION`, the attach `jar=` logs (`attach()`) and the rebuild
  debug log (`rebuild()`) are now listed under `CharacterTabPanelPlugin`. They are removed from the
  `PathTreeBuilder` row, and the `CharacterTabReplacer` row now reads "the `UI_VERSION` log in
  `install()`".
- A7 (resolved): line anchors are replaced with method names in these places:
  - 2a: `SpendService.spend`, `hasAnyInPreviousTier`, `removePendingWithDependents`,
    `commitPending`, `PendingPurchaseTopoSort.sort`.
  - 3a: `configureForViewport(List<TreeDef>,...)`, with the `2*laneGap` note, and `emitLaneColumn`.
  - 5a: `test/skills_overhaul/ui/` exists; the `AdminXPTracker` registration.
  - 5b: full LunaLib paths.
  - 5c: `onGameLoad`, the `AdminXPTracker` registration, the replacer line.
  - The line-numbered criteria in 1a and the "about :N" notes in 4a are kept. Both briefs already
    say to locate code by symbol.
- A8 (resolved): `briefs/5c` and `tasks.yaml` 5c now target the first `## Unreleased` section in
  `CHANGELOG.md`, which today is `## Unreleased — Defense lane spec gap closure (2026-05-10)`, and
  say not to add a new section.
- A9 (resolved): the PRD §8 M5 row, the M5 deliverable in `briefs/5c` and `tasks.yaml` 5c
  `done_when` now include one SiC INFO line and one campaign message naming the hotkey per game
  session. A probe failure, if one occurs, logs one WARN with its reason; otherwise that path is
  checked by code review.
- A10 (resolved): all 8 briefs end `## Execution` with the same gaming-guard line: run
  `~/.claude/hooks/gaming-check.sh` before builds; exit 1 means stop, commit WIP, and report
  `paused: gaming`. The two older, different lines in 3b and 4a are replaced.

## Skipped
- A11: left optional on the caller's instruction. 5c may still add `HostRouterTest`, which is not
  listed in `tasks.yaml`.

## Re-check after apply
- `tasks.yaml` parses (8 tasks), and `waves.sh list` exits 0 with the same waves.
- No two tasks in the same wave share a file. 5a adds `HostEvents.java` and `ReflectionUtils.java`,
  5b adds nothing new, and 5c (wave 6) deletes `ReflectionUtils` only if it is unreferenced.
- Grep of the briefs finds:
  - no "do not commit" lines;
  - exactly one `- Commit:` bullet and one `gaming-check.sh` line in each of the 8 briefs;
  - no `prds/skills-panel-restructure/` paths;
  - none of the stale anchors (`~66-70`, `~line 74`, `:60-65`, `M/LunaLib`, `line 74 today`);
  - no "WARN per distinct" and no "exposed to the caller as results" (5c:13 updated too).
- The 1a method names were checked in code: `attach()` at `CharacterTabPanelPlugin.java:111`,
  `rebuild()` at :133, `processInput` at :1038.
- Every brief's Deliverables table still matches `tasks.yaml`.
- No new findings.
