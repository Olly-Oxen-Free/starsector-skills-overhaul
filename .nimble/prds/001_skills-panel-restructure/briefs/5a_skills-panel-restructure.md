# Task brief: 5a VanillaCharTabProbe and CharTabHost

PRD: skills-panel-restructure | Wave: 5 | Complexity: 6/10 | Model: opus | Priority: must
Agent: implementer

## Objective
Add the host layer that puts `SkillTreeView` into the vanilla CHARACTER tab: `VanillaCharTabProbe` (reflection behind an explicit result type), `MountRetry` (headless, testable arm-once and 180-frame retry state) and `CharTabHost` (arms a deferred mount, swaps the vanilla skill panel for the view in its parent, unmounts when the tab leaves CHARACTER). Nothing is registered in this task; 5c wires it.

## Context
Skills Overhaul (Starsector mod, Java 8, no compiler checks beyond `./build.sh`) is rebuilding its character-tab panel as model / layout / view / host layers. Today `ui/CharacterTabReplacer` (an EveryFrameScript) does reflection, mounting and sibling-parking in one class and installs nothing under Second-in-Command (SiC). This task fulfils PRD requirement R8: a deferred mount in the vanilla panel's parent, armed by either `CoreUITabListener` or a transient poll of `getCurrentCoreTab() == CHARACTER`, with an explicit probe result.

- Upstream: task 4a produces `src/skills_overhaul/ui/view/SkillTreeView.java` (a `CustomUIPanelPlugin` that builds once per tab and is sized by the panel it is given). It does not exist in your worktree if 4a has not merged; check with Glob and read its constructor/factory before coding `CharTabHost`. If the API is unclear, create the panel with `Global.getSettings().createCustom(w, h, plugin)` as `CharacterTabReplacer.install()` does today.
- Downstream: 5b adds `DialogHost` (reuses SkillTreeView); 5c registers `CharTabHost` + the `CoreUITabListener` in `SkillsOverhaulModPlugin`, routes SiC/probe-failure to DialogHost, and deletes `CharacterTabReplacer` (and `ReflectionUtils` only if unused). Expose results (probe failure, SiC detected) to the caller; do not decide routing here.

## Requirements
1. `VanillaCharTabProbe.probe()` returns a result that is either success `{core, vanillaPanel, parent}` or a named failure reason (enum or string constant, e.g. NOT_CAMPAIGN_STATE, NO_CORE, NO_VANILLA_PANEL, NO_PARENT, REFLECTION_ERROR). No reflective exception may be swallowed into an empty list or silent `return`. It wraps the seven reflective calls plus the `CampaignState` / `AppDriver` references: `getEncounterDialog`, `getCoreUI`, `getCore`, `getCurrentTab`, `getParent`, `getChildrenCopy`, `hasMethodOfName("canReassign")`. It may call `ui/ReflectionUtils`. Note `ReflectionUtils.getChildrenCopy` swallows Throwables and returns an empty list, so the probe must distinguish "no children" from "reflection failed" (add a distinguishing path in the probe, or a non-swallowing variant; keep `ReflectionUtils` edits minimal because `CharacterTabReplacer` still uses it until 5c).
2. SiC detection exposed as a result: `Global.getSettings().getModManager().isModEnabled("second_in_command")`, with the sibling class-name scan (`getSimpleName().contains("SCSkillMenu")`) as fallback, as in `CharacterTabReplacer.java:127-145`. Return it to the caller (for example a probe-result field or a separate static method); do not mount when it is true and do not silently return.
3. `MountRetry` (pure Java, no Starsector or GL imports) owns the state machine:
   - Two triggers: listener (`armFromListener()`) and CHARACTER-tab poll (`onPoll(boolean tabIsCharacter, boolean mounted)` or equivalent) both arm one mount.
   - Arm-once-per-open: if both fire for the same tab open, exactly one mount attempt sequence runs.
   - The poll re-arms after the tab has left CHARACTER and opens again.
   - Deferred attempts: each frame the host calls a tick that runs an attempt callback; stops on success; gives up after at most 180 frames; reports exhaustion exactly once per arming (e.g. returns an enum/state EXHAUSTED once, then IDLE).
4. `CharTabHost` is a transient `EveryFrameScript` (`runWhilePaused()` true, `isDone()` false) that also exposes the `CoreUITabListener` callback (`reportAboutToOpenCoreTab(CoreUITabId, Object)`). The callback only calls `MountRetry` arm for `CoreUITabId.CHARACTER`; it must never mount inside the callback (the tab does not exist yet). `advance` polls `Global.getSector().getCampaignUI().getCurrentCoreTab()`: feeds the poll trigger, runs deferred attempts, and when mounted and the tab is no longer CHARACTER, unmounts.
5. Mount: on a successful probe, record the vanilla panel's original size/location, then `parent.removeComponent(vanilla)`, create the SkillTreeView panel sized to the parent's `getPosition().getWidth()/getHeight()` and `parent.addComponent(ourPanel)`, anchored at `inTL(0,0)` (SiC precedent `SCSkillMenuPanel.kt:97-104`; nex4x `IntelDiplomacyTabInjector.java:121-126`). Park no siblings (no `setLocation(-7000,...)`, no `hideSiblings`). Unmount: remove our panel, re-add the vanilla panel to the parent, restore its size/location. Also unmount if the screen size changes while mounted (remount on next open), as the replacer does.
6. Reset all statics on game load (nex4x pattern); prefer instance state so a new host instance is clean.
7. Log: one WARN per distinct probe failure reason per session (`Global.getLogger`, prefix `[SkillsOverhaul/UI] `); exhaustion logs once.

## Success criteria
- [ ] VanillaCharTabProbe.probe() returns a result that is either {core, vanillaPanel, parent} or a named failure reason; no reflective exception is swallowed into an empty list
- [ ] MountRetryTest passes: the deferred-mount scheduler retries for at most 180 frames, stops on success, and reports exhaustion exactly once
- [ ] MountRetryTest also shows that the mount is armed by the listener trigger alone, by the CHARACTER-tab poll alone, and only once when both fire for the same tab open; the poll re-arms after the tab leaves CHARACTER and opens again
- [ ] CharTabHost mounts SkillTreeView sized to the parent's width and height via parent.removeComponent(vanilla) + addComponent, parks no siblings, and unmounts when getCurrentCoreTab() is no longer CHARACTER
- [ ] Probe failure and SiC detection are exposed to the caller as results, not silent returns; ./build.sh and ./build.sh test pass

## Files to modify
| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/host/VanillaCharTabProbe.java` | create | reflective discovery with explicit result type and SiC flag |
| `src/skills_overhaul/ui/host/CharTabHost.java` | create | EveryFrameScript + CoreUITabListener; mount/unmount |
| `src/skills_overhaul/ui/host/MountRetry.java` | create | headless arm-once / 180-frame retry state |
| `test/skills_overhaul/ui/host/MountRetryTest.java` | create | headless assert test (new directory `test/skills_overhaul/ui/host/`) |

`src/skills_overhaul/ui/host/` and `test/skills_overhaul/ui/` do not exist yet (verified). Do not edit `CharacterTabReplacer`, `SkillsOverhaulModPlugin` or delete `ReflectionUtils` (5c).

## Deliverables
None beyond the files above.

## Implementation guidance
- Pattern to follow (reflection and discovery): `src/skills_overhaul/ui/CharacterTabReplacer.java:80-150` (`install()`: `AppDriver.getInstance().getCurrentState()` instanceof `CampaignState`; core via `getEncounterDialog` then `getCoreUI`, else `getCore`; `getCurrentTab` then `findLargestPanelWithCanReassign` (lines ~263-293, largest-area descendant with method `canReassign`), falling back to the search from `core`; parent via `ReflectionUtils.getParent`).
- Pattern to follow (unmount/restore): `CharacterTabReplacer.maybeUninstall()` (restore size/location with `setSuspendRecompute`).
- Pattern to follow (test style): `test/skills_overhaul/tree/PendingPurchaseTopoSortTest.java` - plain `public static void main`, Java `assert`, prints `ALL PASSED`; `./build.sh test` runs `java -ea` on every `*Test` class.
- Research: `CoreUITabListener.reportAboutToOpenCoreTab` fires before the switch completes; always defer, cap `MAX_RESOLVE=180` frames (nex4x `IntelDiplomacyTabInjector.java:38,98-110`). There is no close hook: poll `getCurrentCoreTab()` while mounted (nex4x `:127`).
- Research: whether the listener fires for CHARACTER at all is unknown (in-game only); that is why the poll is a second trigger. SiC's `SkillPanelReplacerScript.kt:31-34` and the current replacer do the same poll.
- Research: SiC removes the same vanilla panel; after SiC removes it no `canReassign` panel remains, so the probe returns NO_VANILLA_PANEL rather than looping silently. Under SiC the caller uses DialogHost.
- Listener registration convention (for 5c, document in a comment): `addListener(l, true)` = transient, guarded by `hasListenerOfClass` (`SkillsOverhaulModPlugin.java:69-70`; nex4x `Nex4xModPlugin.java:359-368`).
- Starsector's script classloader blocks direct `java.lang.reflect.Method/Field` references in mod code; keep all reflection inside `ReflectionUtils` (MethodHandle based) and do not add `java.lang.reflect` imports.
- Keep `MountRetry` free of `com.fs.*` imports so `MountRetryTest` runs headless. Drive it in the test with fake frame ticks and a fake attempt callback.
- Edge cases: tab already CHARACTER when the host starts; tab flips CHARACTER -> other -> CHARACTER (must remount); listener fires and the poll sees CHARACTER on the next frame (one mount only); attempt succeeds on frame 1 and on frame 180 (success) vs never (exactly one exhaustion report, none on later frames until re-armed); parent `getPosition()` zero-sized (treat as failed attempt, retry); `Global.getSector() == null` (return).
- Compile target is Java 8 (`javac -source 8`); no `var`, no `List.of`.

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08, trimmed):
- Campaign listeners: `addListener(l, true)` means transient (not saved); `false` saves a new copy every load. `AdminXPTracker` and `SupplyBufferRenderer` were persistent and stacked until fixed.
- `./build.sh` builds the jar; `./build.sh test` runs every `*Test` class (77 at the time).

## Testing requirements
Implementation task with a test file named here. Run `validate.sh --changed` (targeted tests), then `./build.sh` and `./build.sh test`. `MountRetryTest` must cover:
- retries at most 180 frames when the attempt never succeeds, and reports exhaustion exactly once (not on frame 181+, not twice);
- stops retrying on the first success (no further attempt calls);
- armed by the listener trigger alone; armed by the CHARACTER poll alone;
- both triggers for the same open yield one mount sequence;
- after the tab leaves CHARACTER and opens again, the poll re-arms and mounts again.
The probe and host need Starsector classes and are verified by compile only; the in-game check (M5) belongs to 5c.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar. `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`; do NOT stage or commit that file (restore it with `git checkout -- jars/SkillsOverhaul.jar` before committing).
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (this task should not touch them).
- Out of scope: `DialogHost` and the hotkey (5b); registration, SiC/probe-failure routing, README/CHANGELOG, deleting `CharacterTabReplacer`/`ReflectionUtils` (5c); `zealot/CrusadeNPCStub` reflection; changes to `SkillTreeView`, `Node`/`TreeDef`, `SpendService`.

## Execution
- Branch: `worktree/skills-panel-restructure-5a`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
