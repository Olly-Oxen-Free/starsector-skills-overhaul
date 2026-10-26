# Review: skills-panel-restructure (prd), 2026-10-26

Blocking: 0   Advisory: 8

Structural checks that passed: `tasks.yaml` parses; ids unique; every task has `wave` and
`deliverables`; `waves.sh list` exits 0 (waves 1:[1a] 2:[2a] 3:[3a,3b] 4:[4a] 5:[5a,5b] 6:[5c]);
every task's wave exceeds its dependencies' waves; same-wave tasks (3a/3b, 5a/5b) share no file;
R1-R10 each map to a task; priorities all `must`; `model` matches `complexity_threshold: 5`;
4a at complexity 7 carries a recorded reason (PRD:222). No task lists a `never_touch` path
(`*.jar`, `build/`, `target/`) or a `require_review` file (`mod_info.json`, `build.sh`), and none
lists the PRD §11 no-touch files (`Node.java`, `TreeDef.java`, `SpendService.java`,
`CrusadeNPCStub.java`, tree JSON).

## Blocking

None.

## Disproof attempts

- Decision: view mounts in the vanilla panel's parent, replacing it; SiC or probe failure uses
  DialogHost via hotkey. **Held.** SiC 2.0.0 removes the same panel from the same parent
  (`game/mods/Second-in-Command-2.0.0/src/second_in_command/scripts/SkillPanelReplacerScript.kt:58-63`),
  and the current code already defers to SiC by mod id `second_in_command`
  (`src/skills_overhaul/ui/CharacterTabReplacer.java:127-145`; id confirmed in
  `game/mods/Second-in-Command-2.0.0/mod_info.json:2`). The API supports the dialog path
  (`CampaignUIAPI.showInteractionDialog(InteractionDialogPlugin, SectorEntityToken)`,
  `InteractionDialogAPI.showCustomVisualDialog(float, float, CustomVisualDialogDelegate)` in
  `game/starfarer.api.zip`). See A1 for the trigger mechanism, which is a separate choice.
- Decision: confirm is all-or-nothing through a dry run on one cloned whole-tab snapshot.
  **Held.** `SpendService.spend` returns at `src/skills_overhaul/level/SpendService.java:57-61`
  before any `mem.put` (writes start at :64), and a successful spend sets the purchased flag
  (:69), so sequential spends on one clone see earlier batch nodes as owned for
  `PrereqValidator.canPurchase` (`src/skills_overhaul/tree/PrereqValidator.java:9-14`).
  `TreeRegistry.getTrees(Tree)` exists for the whole-tab snapshot (`TreeRegistry.java:30`).
- Decision: bottom-up panel-local coordinates everywhere. **Held.** The vanilla example uses
  `event.getY()` in the same space as `p.getY()` with no flip and draws quads from `y` to `y + h`
  (`game/starfarer.api.zip` -> `com/fs/starfarer/api/impl/campaign/ExampleCustomUIPanel.java:27-28,44-45,76-79`).
  The contrary comments in the current code (`CharacterTabPanelPlugin.java:511-513`, `:990-1001`)
  are the bug this PRD fixes.

## Advisory

### A1. CoreUITabListener not firing is not a probe failure, so the fallback never announces itself
- Where: `tasks.yaml:207-209` (5a description), `tasks.yaml:287` (5c), `PRD_skills-panel-restructure.md:105,156,167`
- Why: The PRD's mitigation for "listener does not fire for CHARACTER" is "probe failure routes
  to DialogHost". If the listener never fires, the probe never runs, no WARN or campaign message
  appears, and the player sees the vanilla panel with no hint. Both existing CHARACTER-tab
  precedents poll instead of listening: SiC's transient `EveryFrameScript` checks
  `currentCoreTab == CHARACTER` (`SkillPanelReplacerScript.kt:21-34`), as does the current
  `CharacterTabReplacer` (`CharacterTabReplacer.java:25,49`). The nex4x listener precedent is for
  INTEL, not CHARACTER (`workspace/nexerelin-4x-mod/src/nex4x/ui/CoreUITabInjectorListener.java:52`).
  This might be blocking for R8 if the in-game check shows the listener does not fire.
- Fix: In 5a, drive CharTabHost from a transient `EveryFrameScript` (runWhilePaused) that polls
  `getCurrentCoreTab() == CHARACTER`, or keep the listener and add that poll as the backstop. Then
  PRD §9 Q1 stops gating the fallback.

### A2. SiC players are never told about the hotkey
- Where: `PRD_skills-panel-restructure.md:139-140`; `tasks.yaml:286` (5c criterion 2)
- Why: Under SiC, the case R9 exists for, 5c logs one INFO line only. The campaign message naming
  the hotkey appears only on probe failure. Neither `README.md` nor `CHANGELOG.md` mentions a
  hotkey or `skills_overhaul.json` keys (grep finds no `hotkey`, `migration_cap` or
  `skills_overhaul.json`). A SiC player has no in-game way to find `K`.
- Fix: Change the 5c criterion to show the one-per-session campaign message under SiC too, and add
  a `CHANGELOG.md` entry naming `ui_hotkey` to 5b or 5c deliverables.

### A3. New-behaviour tests without a recorded user ask
- Where: `tasks.yaml:65,112,144,217,250-251`; `PRD_skills-panel-restructure.md:69,90`
- Why: `.nimble/config.yaml` does not set `tests.new_behaviour: tester`, and neither the PRD nor
  `interview.md` records the user asking for tests. "Headless tests" comes from
  `research/summary.md:13-14`. PanelCoordsTest (1a, y-axis bug) and PurchasePlanTest and the
  SpendServiceTest assertion (2a, partial commit) are regression tests and are justified by
  `tests.regression_per_fix: true`. SkillTreeModelTest, TreeLayoutTest, NodeTooltipContentTest,
  MountRetryTest, SkillsHotkeyGuardTest and the ConfigTest default assertion cover new behaviour.
  The success metric also says "6 classes" while the tasks create 7 new test classes.
- Fix: If the user's increment plan asked for headless tests, record that in PRD §9 as an
  `Assumption:` or user ask and set the metric to 7. If not, keep the two regression tests and
  drop the new-behaviour test criteria.

### A4. 1a grep criterion passes before any change
- Where: `tasks.yaml:22`
- Why: "no remaining `getScreenHeight() -` y conversion" already holds today. The flip is written
  `sh - (float) e.getY()` after `float sh = Global.getSettings().getScreenHeight()`
  (`CharacterTabPanelPlugin.java:998-1000`), so the grep matches nothing. The legitimate
  `getScreenHeight()` at `:583` (scissor viewport scale) would also make a broader grep fail.
- Fix: Reword to "eventToPanelLocalY (or its PanelCoords replacement) returns `e.getY() - py`
  with no screen-height flip; the only `getScreenHeight()` left in `CharacterTabPanelPlugin` is
  the scissor viewport scale".

### A5. 2a says PrereqValidatorTest passes unchanged but lists it as affected
- Where: `tasks.yaml:68` versus `tasks.yaml:79`
- Why: The criterion says "PrereqValidatorTest passes unchanged", and `files_affected` lists
  `test/skills_overhaul/tree/PrereqValidatorTest.java`. An agent can't tell whether to add the
  reason-method cases there.
- Fix: Reword to "existing PrereqValidatorTest assertions are unchanged; new assertions cover the
  reason-returning method", or remove the file from `files_affected`.

### A6. TreeLayoutTest's 3-TreeDef case is lighter than the real worst case
- Where: `tasks.yaml:112`
- Why: The commander tab has 5 TreeDefs of 21-22 nodes each (x 0..4, y 0..5) in
  `data/config/skills_overhaul/trees/commander_{artificer,collective,commandant,corsair,zealot}.json`.
  The admin tab has 4 trees of 2 nodes each at x=2. A 3-tree fixture does not show that fit works
  for the widest tab, which is the case that matters when the view fills the vanilla parent (5a).
  `TreeDef.java:18-27` only rejects duplicate coordinates and does not enforce the 0..4/0..5 bounds
  that `tasks.yaml:126` attributes to it.
- Fix: Make the fixture 5 TreeDefs (one sparse, like admin) and cite the tree JSON, not
  `TreeDef.java:20-27`, for the grid bounds.

### A7. `./build.sh` rewrites a tracked never_touch jar
- Where: every task's `./build.sh` criterion; `.nimble/config.yaml` `boundaries.never_touch: "*.jar"`
- Why: `build.sh:9,75-76` writes `jars/SkillsOverhaul.jar`, which git tracks (it is not in
  `.gitignore`; only `jars/classes/` is), and `git status` already shows it modified. A
  task agent that stages broadly commits a never_touch file, and the code reviewer would block it.
- Fix: Add to each brief: "do not stage `jars/`; run `git checkout -- jars/SkillsOverhaul.jar`
  before committing". Alternatively, the user can decide whether the jar stays tracked.

### A8. Line references into files that 1a rewrites will be stale by waves 2-4
- Where: `tasks.yaml:87,90-91` (2a: `CharacterTabPanelPlugin.java:937-948, :1208-1242, :1254-1266`),
  `tasks.yaml:157` (3b: `PathTreeBuilder.java:57-62`), `tasks.yaml:197` (4a: `:1166-1200`)
- Why: 1a deletes dead fields, methods and log blocks in both files (for example
  `SUBLANE_W` at `PathTreeBuilder.java:45`, and `bbLevel/bbPoints` and the UI_VERSION logs in
  `CharacterTabPanelPlugin.java:45,117-135,454-455`), so every later line number shifts.
- Fix: Name the anchors instead: `hasAnyInPreviousTier`, the budget block in the node-click
  handler, `commitPending`, the `COLOR_OWNED..COLOR_CAPSTONE` constants, and the owned-T0 branch
  with `TODO(active-basics)`.

## Checks requested by caller

- **Default hotkey `K` (LWJGL keycode 37).** No collision found in campaign bindings. Vanilla
  rebindable controls live in the Java prefs store, not `data/config/settings.json`
  (`~/.java/.userPrefs/com/fs/starfarer/prefs.xml`, `controls1`+`controls2`, 220 entries). No
  entry uses code 37. The game's `data/config/settings.json` defines no key bindings. Two enabled
  mods bind 37, both in combat only, which a `CampaignInputListener` does not see: Advanced
  Gunnery Control `agc_mergeHotkey` and Flux Reticle `sun_fr_toggleStrafeAndTurnToCursorKey`
  (`saves/common/LunaSettings/*.json.data`). SiC 2.0.0 binds no K. Residual: vanilla's fixed
  core-tab shortcuts are compiled into `starfarer_obf.jar` and were not decompiled; M5's "vanilla
  bindings still work" check covers this.
- **never_touch paths.** No task's `files_affected` names `*.jar`, `build/` or `target/`. See A7
  for the jar that `./build.sh` writes as a side effect.

## Applied
- A1: tasks.yaml 5a - description, a new MountRetryTest criterion and agent_context make CharTabHost arm the mount from either the CoreUITabListener callback or a transient every-frame `getCurrentCoreTab() == CHARACTER` poll, with one arm-once-per-open state in MountRetry. tasks.yaml 5c - registration criterion adds the poll script via `addTransientScript`, guarded by `hasTransientScript` (`SectorAPI.java:337`). PRD R8 (§4), external blocker row (§6), listener risk row (§6) and Q1 (§9) updated to match.
- A2: tasks.yaml 5c - description and criteria show a once-per-game-session campaign message naming the hotkey under SiC and on probe failure (shared flag, never shown twice). A new criterion and deliverable add README.md and CHANGELOG.md lines, and both files are added to files_affected. The agent_context names `CampaignUIAPI.addMessage(String)`. PRD R9 and the §5 error state updated.
- A3 (count only): PRD §3 metric changed from "6 classes" to "7 new classes".
- A4: tasks.yaml 1a - the grep criterion now requires the cursor y conversion to return `e.getY() - py` with no screen-height flip, with the scissor viewport scale as the only `getScreenHeight()` left.
- A5: tasks.yaml 2a - the criterion now says existing PrereqValidatorTest assertions stay unchanged and new assertions cover the reason-returning method.
- A6: tasks.yaml 3a - the fixture is the 5 real commander TreeDefs (21-22 nodes each), loaded like TreeLoaderRealJSONTest (`findModRoot`). The unenforced `TreeDef.java:20-27` bounds claim is replaced by the tree JSON paths.
- A8: tasks.yaml 2a, 3b and 4a - line anchors replaced with names: `commitPending`, `hasAnyInPreviousTier`, the `totalPts`/`sumPendingCost` over-budget block, the `COLOR_*` constants, the `TODO(active-basics)` branch, the SpendService `remaining > 0` early return (the old `SpendService.java:66-82` anchor was also wrong; the return is at :57-61) and `Prereq.All/Any/AtLeast`.
- Verified: tasks.yaml parses; `waves.sh list` exits 0 with the same six waves; a grep for the removed strings (`CharacterTabPanelPlugin.java:N`, `PathTreeBuilder.java:N`, `TreeDef.java:20`, `getScreenHeight() -`, `passes unchanged`, `for 3 TreeDefs`, `6 classes`) returns nothing.

## Skipped
- A7: left as a recorded note, by user choice. `./build.sh` still rewrites the tracked `jars/SkillsOverhaul.jar`. Before execution, the user must decide whether briefs tell agents to restore it before committing, or whether the jar stops being tracked.
- A3 (beyond the count): the user has not decided whether the new-behaviour tests stay. The PRD still records no user request for them.

## Re-review (changed documents), 2026-10-26

Blocking: 0   Advisory: 3 (A3 still open, A7 still recorded, plus R1 below)

Changed sections re-checked: 1a, 2a, 3a, 3b, 4a, 5a and 5c in tasks.yaml, and PRD §3, §4, §5, §6, §9.
- 5a: the new criterion can be checked headlessly through MountRetry. Its files_affected is unchanged because the poll lives in CharTabHost. No file is shared with 5b.
- 5c: README.md and CHANGELOG.md are new to this task and no other task in wave 6 touches them. `hasTransientScript(Class)` and `addMessage(String)` exist in `game/starfarer.api.zip`. Complexity 4 still fits.
- 3a: the fixture can be loaded; `test/skills_overhaul/tree/TreeLoaderRealJSONTest.java:42-46` locates the mod root from jars/classes.
- The PRD and tasks.yaml now agree on the R8 trigger, the SiC message and the test count.

### R1. CharacterTabReplacer line anchors in 5a and 5c will drift (new, advisory)
- Where: `tasks.yaml:235-236` (5a: `:81-134, :230-293, :263-293`), `tasks.yaml:319` (5c: `replacer :127-145`)
- Why: 1a edits `CharacterTabReplacer.java` (UI_VERSION log at :214) and 4a edits it again before waves 5-6, so these line numbers will be stale. This is the same problem as A8 but in tasks A8 did not cover.
- Fix: Name the anchors instead: `findLargestPanelWithCanReassign`, the SiC check (`isModEnabled("second_in_command")` plus the `SCSkillMenu` sibling scan), and the reflective core lookup in `advance`.
